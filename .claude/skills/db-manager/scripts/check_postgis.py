#!/usr/bin/env python3
# .claude/skills/db-manager/scripts/check_postgis.py
# PostGIS 쿼리 검증 스크립트

import re
import sys
import json
from typing import List, Dict, Any, Set


# 지원되는 PostGIS 함수 목록
SUPPORTED_POSTGIS_FUNCTIONS: Set[str] = {
    # 기하학 생성 (Geometry Constructors)
    'ST_Point', 'ST_MakePoint', 'ST_GeomFromText', 'ST_GeomFromGeoJSON',
    'ST_GeogFromText', 'ST_MakePolygon', 'ST_MakeLine', 'ST_MakeEnvelope',
    'ST_GeomFromWKB', 'ST_GeomFromEWKT', 'ST_MakeBox2D',

    # 측정 (Measurements)
    'ST_Distance', 'ST_DWithin', 'ST_Length', 'ST_Area', 'ST_Perimeter',
    'ST_Azimuth', 'ST_Angle',

    # 공간 관계 (Spatial Relationships)
    'ST_Contains', 'ST_Within', 'ST_Intersects', 'ST_Overlaps',
    'ST_Touches', 'ST_Crosses', 'ST_Disjoint', 'ST_Equals',
    'ST_CoveredBy', 'ST_Covers', 'ST_ContainsProperly',

    # 변환 (Transformations)
    'ST_Transform', 'ST_SetSRID', 'ST_AsText', 'ST_AsGeoJSON',
    'ST_X', 'ST_Y', 'ST_Centroid', 'ST_Buffer', 'ST_Envelope',
    'ST_AsEWKT', 'ST_AsBinary', 'ST_AsEWKB', 'ST_SRID',

    # 집계 (Aggregates)
    'ST_Union', 'ST_Collect', 'ST_ConvexHull', 'ST_Extent',

    # 편집 (Editors)
    'ST_AddPoint', 'ST_RemovePoint', 'ST_SetPoint', 'ST_Reverse',
    'ST_Simplify', 'ST_SimplifyPreserveTopology',

    # 접근자 (Accessors)
    'ST_GeometryType', 'ST_NDims', 'ST_NPoints', 'ST_NRings',
    'ST_NumGeometries', 'ST_NumPoints', 'ST_Dimension',

    # 최근접 이웃 (Nearest Neighbor)
    'ST_ClosestPoint', 'ST_ShortestLine', 'ST_LongestLine'
}

# SRID 유효성 (한국 좌표계 포함)
VALID_SRIDS: Dict[int, str] = {
    4326: 'WGS 84 (GPS)',
    5186: 'Korea 2000 / Central Belt',
    5187: 'Korea 2000 / East Belt',
    5188: 'Korea 2000 / East Sea Belt',
    5179: 'Korea 2000 / Unified CS',
    3857: 'Web Mercator (Google Maps)',
    32652: 'WGS 84 / UTM zone 52N',
}


def validate_postgis_query(query: str) -> Dict[str, Any]:
    """PostGIS 쿼리 유효성 검증"""
    results: Dict[str, Any] = {
        'valid': True,
        'warnings': [],
        'errors': [],
        'suggestions': [],
        'detected_functions': [],
        'detected_srids': [],
        'performance_notes': []
    }

    query_upper = query.upper()

    # PostGIS 함수 탐지
    function_pattern = r'(ST_\w+)\s*\('
    found_functions = re.findall(function_pattern, query, re.IGNORECASE)

    for func in found_functions:
        results['detected_functions'].append(func)

        if func.upper() not in [f.upper() for f in SUPPORTED_POSTGIS_FUNCTIONS]:
            results['warnings'].append(f"알 수 없는 PostGIS 함수: {func}")

    results['detected_functions'] = list(set(results['detected_functions']))

    # SRID 탐지
    srid_patterns = [
        r'SRID\s*=\s*(\d+)',
        r'ST_SetSRID\s*\([^,]+,\s*(\d+)',
        r'ST_Transform\s*\([^,]+,\s*(\d+)',
        r'ST_GeomFromText\s*\([^,]+,\s*(\d+)'
    ]

    found_srids: Set[int] = set()
    for pattern in srid_patterns:
        matches = re.findall(pattern, query, re.IGNORECASE)
        for match in matches:
            found_srids.add(int(match))

    results['detected_srids'] = list(found_srids)

    for srid in found_srids:
        if srid not in VALID_SRIDS:
            results['warnings'].append(f"비표준 SRID 감지: {srid}")
        else:
            results['suggestions'].append(f"SRID {srid}: {VALID_SRIDS[srid]}")

    # 성능 관련 검사
    _check_performance(query, query_upper, results)

    # Geography vs Geometry 검사
    _check_geography_geometry(query, query_upper, results)

    # 인덱스 활용 검사
    _check_index_usage(query, query_upper, results)

    # SQL Injection 위험 패턴 검사
    _check_security(query, results)

    return results


def _check_performance(query: str, query_upper: str, results: Dict[str, Any]) -> None:
    """성능 관련 검사"""

    # ST_Distance vs ST_DWithin
    if 'ST_DISTANCE' in query_upper and 'ST_DWITHIN' not in query_upper:
        if 'WHERE' in query_upper and ('>' in query or '<' in query):
            results['performance_notes'].append(
                "💡 ST_Distance로 거리 필터링 시 ST_DWithin 사용 권장 (공간 인덱스 활용)"
            )

    # 다중 ST_Transform
    transform_count = query_upper.count('ST_TRANSFORM')
    if transform_count > 1:
        results['performance_notes'].append(
            f"⚠️ ST_Transform이 {transform_count}회 호출됨. 데이터 사전 변환 고려"
        )

    # SELECT *
    if 'SELECT *' in query_upper and 'GEOMETRY' in query_upper:
        results['performance_notes'].append(
            "💡 SELECT * 대신 필요한 컬럼만 지정 권장 (대용량 Geometry 전송 방지)"
        )

    # ORDER BY with spatial function
    if 'ORDER BY' in query_upper and any(
        f in query_upper for f in ['ST_DISTANCE', 'ST_LENGTH', 'ST_AREA']
    ):
        results['performance_notes'].append(
            "💡 공간 함수로 정렬 시 KNN 연산자(<->) 사용 고려"
        )


def _check_geography_geometry(query: str, query_upper: str, results: Dict[str, Any]) -> None:
    """Geography vs Geometry 타입 검사"""

    has_distance = any(f in query_upper for f in ['ST_DISTANCE', 'ST_DWITHIN'])
    has_4326 = 4326 in results['detected_srids']
    uses_geography = 'GEOGRAPHY' in query_upper

    if has_distance and has_4326 and not uses_geography:
        results['suggestions'].append(
            "🌍 SRID 4326(WGS84)으로 거리 계산 시 Geography 타입 사용 권장 (더 정확한 지구 표면 거리)"
        )


def _check_index_usage(query: str, query_upper: str, results: Dict[str, Any]) -> None:
    """인덱스 활용 가능성 검사"""

    spatial_predicates = ['ST_CONTAINS', 'ST_WITHIN', 'ST_INTERSECTS', 'ST_DWITHIN', 'ST_COVEREDBY']

    if 'WHERE' in query_upper:
        used_predicates = [p for p in spatial_predicates if p in query_upper]
        if used_predicates:
            results['suggestions'].append(
                f"✅ 공간 조건자 사용됨: {', '.join(used_predicates)} - GiST 인덱스 활용 가능"
            )


def _check_security(query: str, results: Dict[str, Any]) -> None:
    """보안 관련 검사"""

    # 문자열 연결 패턴 (잠재적 SQL Injection)
    dangerous_patterns = [
        r'\|\|.*\$',  # 문자열 연결 + 변수
        r"'\s*\+\s*",  # 문자열 연결
    ]

    for pattern in dangerous_patterns:
        if re.search(pattern, query):
            results['warnings'].append(
                "🔒 잠재적 SQL Injection 위험: 파라미터 바인딩 사용 권장"
            )
            break


def print_results(results: Dict[str, Any], query: str) -> None:
    """결과 출력"""
    print("=" * 60)
    print("🔍 PostGIS Query Validation Report")
    print("=" * 60)

    print(f"\n📝 Query Preview:")
    print(f"   {query[:100]}..." if len(query) > 100 else f"   {query}")

    print(f"\n📊 Overall: {'✅ VALID' if results['valid'] else '❌ INVALID'}")

    if results['detected_functions']:
        print(f"\n📌 Detected PostGIS Functions ({len(results['detected_functions'])}):")
        for func in sorted(set(results['detected_functions'])):
            print(f"   • {func}")

    if results['detected_srids']:
        print(f"\n🌐 Detected SRIDs:")
        for srid in results['detected_srids']:
            desc = VALID_SRIDS.get(srid, "Unknown")
            print(f"   • {srid}: {desc}")

    if results['errors']:
        print(f"\n❌ Errors ({len(results['errors'])}):")
        for error in results['errors']:
            print(f"   • {error}")

    if results['warnings']:
        print(f"\n⚠️ Warnings ({len(results['warnings'])}):")
        for warning in results['warnings']:
            print(f"   • {warning}")

    if results['performance_notes']:
        print(f"\n⚡ Performance Notes:")
        for note in results['performance_notes']:
            print(f"   {note}")

    if results['suggestions']:
        print(f"\n💡 Suggestions:")
        for suggestion in results['suggestions']:
            print(f"   {suggestion}")

    print("\n" + "=" * 60)


def main():
    if len(sys.argv) < 2:
        print("Usage: check_postgis.py <query>")
        print("       check_postgis.py --file <sql_file>")
        print("")
        print("Examples:")
        print("  check_postgis.py \"SELECT ST_Distance(a.geom, b.geom) FROM ...\"")
        print("  check_postgis.py --file stadium_query.sql")
        sys.exit(1)

    if sys.argv[1] == '--file':
        if len(sys.argv) < 3:
            print("Error: SQL file path required")
            sys.exit(1)
        try:
            with open(sys.argv[2], 'r', encoding='utf-8') as f:
                query = f.read()
        except FileNotFoundError:
            print(f"Error: File not found: {sys.argv[2]}")
            sys.exit(1)
    else:
        query = ' '.join(sys.argv[1:])

    result = validate_postgis_query(query)
    print_results(result, query)

    print("\n--- JSON Result ---")
    print(json.dumps(result, indent=2, ensure_ascii=False))

    # 에러가 있으면 exit code 1
    sys.exit(0 if result['valid'] and not result['errors'] else 1)


if __name__ == '__main__':
    main()
