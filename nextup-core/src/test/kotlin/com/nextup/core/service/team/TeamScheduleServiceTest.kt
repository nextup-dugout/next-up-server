package com.nextup.core.service.team

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TeamScheduleService 인터페이스 계약 테스트
 *
 * 구현체(TeamScheduleServiceImpl) 테스트는
 * nextup-infrastructure 모듈의 TeamScheduleServiceImplTest에서 수행합니다.
 */
@DisplayName("TeamScheduleService 인터페이스")
class TeamScheduleServiceTest {
    @Test
    fun `인터페이스는 필요한 메서드를 모두 정의한다`() {
        // TeamScheduleService 인터페이스가 올바르게 정의되어 있는지 컴파일 타임 검증
        val methods = TeamScheduleService::class.java.declaredMethods.map { it.name }.toSet()

        assertThat(methods).containsAll(
            listOf("create", "getByTeamId", "getByTeamIdAndDateRange", "getById", "update", "delete"),
        )
    }
}
