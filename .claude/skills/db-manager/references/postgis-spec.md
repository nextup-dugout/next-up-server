# PostGIS 참조 가이드 for NEXT-UP

## 개요

NEXT-UP 프로젝트에서 사용하는 PostGIS 함수 및 패턴 가이드입니다.

## 좌표계 (SRID)

### 프로젝트 표준

| SRID | 이름 | 용도 |
|------|------|------|
| **4326** | WGS 84 | GPS 좌표, 기본 저장 형식 |
| 5179 | Korea 2000 Unified | 한국 평면 좌표 (거리 계산) |
| 3857 | Web Mercator | 웹 지도 표시 |

### 사용 예시

```sql
-- WGS84 좌표로 Point 생성
SELECT ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326);

-- 좌표계 변환 (WGS84 → Korea 2000)
SELECT ST_Transform(location, 5179) FROM stadiums;
```

## 거리 계산

### ST_DWithin (권장)

공간 인덱스를 활용하여 효율적인 거리 필터링

```sql
-- 특정 지점에서 5km 이내 야구장 검색
SELECT *
FROM stadiums
WHERE ST_DWithin(
    location::geography,
    ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326)::geography,
    5000  -- 미터 단위
);
```

### ST_Distance

정확한 거리 계산

```sql
-- 두 지점 간 거리 (미터)
SELECT ST_Distance(
    location::geography,
    ST_SetSRID(ST_MakePoint(127.0276, 37.4979), 4326)::geography
) AS distance_meters
FROM stadiums;
```

### Geometry vs Geography

| 타입 | 좌표 단위 | 거리 단위 | 사용 시나리오 |
|------|-----------|-----------|---------------|
| Geometry | 도 (degree) | 도 (degree) | 평면 지도, 작은 영역 |
| Geography | 도 (degree) | 미터 (meter) | 지구 표면, 정확한 거리 |

**NEXT-UP 권장**: 거리 계산 시 항상 `geography` 타입 사용

## 공간 관계

### 포함 관계

```sql
-- 서울시 내 야구장
SELECT *
FROM stadiums s
JOIN regions r ON ST_Within(s.location, r.boundary)
WHERE r.name = '서울특별시';
```

### 교차 관계

```sql
-- 특정 영역과 겹치는 야구장
SELECT *
FROM stadiums
WHERE ST_Intersects(
    location,
    ST_MakeEnvelope(126.5, 37.0, 127.5, 38.0, 4326)
);
```

## 인덱스 전략

### GiST 인덱스 (필수)

```sql
-- 공간 컬럼에 GiST 인덱스 생성
CREATE INDEX idx_stadiums_location
ON stadiums USING GIST (location);

-- Geography 타입
CREATE INDEX idx_stadiums_location_geog
ON stadiums USING GIST ((location::geography));
```

### 인덱스 활용 연산자

| 연산자 | 설명 | 인덱스 활용 |
|--------|------|-------------|
| `&&` | Bounding Box 겹침 | ✅ |
| `ST_DWithin` | 거리 이내 | ✅ |
| `ST_Intersects` | 교차 | ✅ |
| `ST_Contains` | 포함 | ✅ |
| `ST_Distance` | 거리 계산 | ❌ (결과만) |

## NEXT-UP 테이블 패턴

### Stadium (야구장) 테이블

```sql
CREATE TABLE stadiums (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    location GEOMETRY(Point, 4326) NOT NULL,
    capacity INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stadiums_location ON stadiums USING GIST (location);
```

### 거리 기반 검색 쿼리

```sql
-- 현재 위치에서 가까운 순으로 야구장 조회
SELECT
    id,
    name,
    ST_Distance(
        location::geography,
        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
    ) AS distance_meters
FROM stadiums
WHERE ST_DWithin(
    location::geography,
    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
    :max_distance_meters
)
ORDER BY distance_meters
LIMIT :limit;
```

## JPA/Hibernate 매핑

### Entity 정의

```kotlin
import org.locationtech.jts.geom.Point

@Entity
@Table(name = "stadiums")
class Stadium(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    val name: String,

    @Column(columnDefinition = "geometry(Point, 4326)")
    val location: Point
) : BaseTimeEntity()
```

### Repository 쿼리

```kotlin
interface StadiumRepository : JpaRepository<Stadium, Long> {

    @Query("""
        SELECT s FROM Stadium s
        WHERE function('ST_DWithin',
            function('cast', s.location, 'geography'),
            function('ST_SetSRID', function('ST_MakePoint', :lng, :lat), 4326),
            :distanceMeters) = true
    """)
    fun findNearby(
        @Param("lng") lng: Double,
        @Param("lat") lat: Double,
        @Param("distanceMeters") distanceMeters: Double
    ): List<Stadium>
}
```

## 성능 최적화 체크리스트

- [ ] 모든 공간 컬럼에 GiST 인덱스 생성
- [ ] 거리 필터링 시 ST_DWithin 사용
- [ ] Geography 타입으로 정확한 거리 계산
- [ ] Bounding Box 연산자(`&&`) 선행 필터링 활용
- [ ] EXPLAIN ANALYZE로 쿼리 플랜 확인
