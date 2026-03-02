package com.nextup.core.port.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.stadium.Stadium

/**
 * 구장 리포지토리 포트 인터페이스
 */
interface StadiumRepositoryPort {
    fun save(stadium: Stadium): Stadium

    fun findByIdOrNull(id: Long): Stadium?

    fun findAll(): List<Stadium>

    fun findAllActive(): List<Stadium>

    /**
     * 특정 위치 근처의 구장을 검색합니다 (PostGIS 활용).
     *
     * @param latitude 위도
     * @param longitude 경도
     * @param radiusKm 검색 반경 (킬로미터)
     * @return 검색 반경 내의 구장 목록
     */
    fun findNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
    ): List<Stadium>

    /**
     * 특정 위치 근처의 구장을 페이징하여 거리 순으로 검색합니다 (PostGIS 활용).
     *
     * @param latitude 위도 (-90 ~ 90)
     * @param longitude 경도 (-180 ~ 180)
     * @param radiusKm 검색 반경 (킬로미터, 0 초과)
     * @param pageCommand 페이징 정보
     * @return 거리 순으로 정렬된 구장 페이지
     */
    fun findNearbyStadiums(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        pageCommand: PageCommand,
    ): PageResult<Stadium>
}
