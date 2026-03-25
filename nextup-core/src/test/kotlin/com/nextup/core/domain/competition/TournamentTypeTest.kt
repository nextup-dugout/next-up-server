package com.nextup.core.domain.competition

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TournamentType 테스트")
class TournamentTypeTest {
    @Test
    fun `SINGLE_ELIMINATION의 displayName이 올바르다`() {
        assertThat(TournamentType.SINGLE_ELIMINATION.displayName).isEqualTo("단일 토너먼트")
    }

    @Test
    fun `TournamentType에는 SINGLE_ELIMINATION만 존재한다`() {
        val values = TournamentType.entries
        assertThat(values).hasSize(1)
        assertThat(values).containsExactly(TournamentType.SINGLE_ELIMINATION)
    }

    @Test
    fun `valueOf로 SINGLE_ELIMINATION을 조회할 수 있다`() {
        val type = TournamentType.valueOf("SINGLE_ELIMINATION")
        assertThat(type).isEqualTo(TournamentType.SINGLE_ELIMINATION)
    }
}
