package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.TeamBlacklist
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

@DisplayName("TeamBlacklistRepositoryAdapter 테스트")
class TeamBlacklistRepositoryAdapterTest {

    private val jpaRepository: TeamBlacklistRepository = mockk()
    private lateinit var adapter: TeamBlacklistRepositoryAdapter

    @BeforeEach
    fun setUp() {
        adapter = TeamBlacklistRepositoryAdapter(jpaRepository)
    }

    @Test
    @DisplayName("save - TeamBlacklist 저장 시 JPA repository에 위임한다")
    fun `should delegate save to jpa repository`() {
        // given
        val blacklist = mockk<TeamBlacklist>()
        every { jpaRepository.save(blacklist) } returns blacklist

        // when
        val result = adapter.save(blacklist)

        // then
        assertThat(result).isEqualTo(blacklist)
        verify(exactly = 1) { jpaRepository.save(blacklist) }
    }

    @Test
    @DisplayName("findByIdOrNull - ID로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByIdOrNull to jpa repository`() {
        // given
        val blacklist = mockk<TeamBlacklist>()
        every { jpaRepository.findByIdOrNull(1L) } returns blacklist

        // when
        val result = adapter.findByIdOrNull(1L)

        // then
        assertThat(result).isEqualTo(blacklist)
        verify(exactly = 1) { jpaRepository.findByIdOrNull(1L) }
    }

    @Test
    @DisplayName("findByIdOrNull - 존재하지 않는 ID 조회 시 null을 반환한다")
    fun `should return null when id not found`() {
        // given
        every { jpaRepository.findByIdOrNull(99L) } returns null

        // when
        val result = adapter.findByIdOrNull(99L)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("findByTeamId - teamId로 페이지 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamId to jpa repository`() {
        // given
        val pageable = mockk<Pageable>()
        val blacklist = mockk<TeamBlacklist>()
        val page = PageImpl(listOf(blacklist))
        every { jpaRepository.findByTeamId(1L, pageable) } returns page

        // when
        val result = adapter.findByTeamId(1L, pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0]).isEqualTo(blacklist)
        verify(exactly = 1) { jpaRepository.findByTeamId(1L, pageable) }
    }

    @Test
    @DisplayName("findByTeamIdAndUserId - teamId와 userId로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamIdAndUserId to jpa repository`() {
        // given
        val blacklist = mockk<TeamBlacklist>()
        every { jpaRepository.findByTeamIdAndUserId(1L, 2L) } returns blacklist

        // when
        val result = adapter.findByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isEqualTo(blacklist)
        verify(exactly = 1) { jpaRepository.findByTeamIdAndUserId(1L, 2L) }
    }

    @Test
    @DisplayName("findByTeamIdAndUserId - 블랙리스트 항목이 없을 때 null을 반환한다")
    fun `should return null when blacklist entry not found`() {
        // given
        every { jpaRepository.findByTeamIdAndUserId(1L, 2L) } returns null

        // when
        val result = adapter.findByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("existsActiveByTeamIdAndUserId - 활성 블랙리스트 존재 여부 확인 시 현재 시각을 전달하여 JPA repository에 위임한다")
    fun `should delegate existsActiveByTeamIdAndUserId with current time to jpa repository`() {
        // given
        val nowSlot = slot<LocalDateTime>()
        val beforeCall = LocalDateTime.now().minusSeconds(1)
        every {
            jpaRepository.existsActiveByTeamIdAndUserIdWithTime(1L, 2L, capture(nowSlot))
        } returns true

        // when
        val result = adapter.existsActiveByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isTrue()
        verify(exactly = 1) {
            jpaRepository.existsActiveByTeamIdAndUserIdWithTime(1L, 2L, any())
        }
        // 전달된 시각이 호출 전후 사이의 값인지 검증
        val capturedTime = nowSlot.captured
        val afterCall = LocalDateTime.now().plusSeconds(1)
        assertThat(capturedTime).isAfter(beforeCall)
        assertThat(capturedTime).isBefore(afterCall)
    }

    @Test
    @DisplayName("existsActiveByTeamIdAndUserId - 활성 블랙리스트가 없을 때 false를 반환한다")
    fun `should return false when no active blacklist exists`() {
        // given
        every {
            jpaRepository.existsActiveByTeamIdAndUserIdWithTime(1L, 2L, any())
        } returns false

        // when
        val result = adapter.existsActiveByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("delete - TeamBlacklist 삭제 시 JPA repository에 위임한다")
    fun `should delegate delete to jpa repository`() {
        // given
        val blacklist = mockk<TeamBlacklist>()
        justRun { jpaRepository.delete(blacklist) }

        // when
        adapter.delete(blacklist)

        // then
        verify(exactly = 1) { jpaRepository.delete(blacklist) }
    }

    @Test
    @DisplayName("deleteById - ID로 삭제 시 JPA repository에 위임한다")
    fun `should delegate deleteById to jpa repository`() {
        // given
        justRun { jpaRepository.deleteById(1L) }

        // when
        adapter.deleteById(1L)

        // then
        verify(exactly = 1) { jpaRepository.deleteById(1L) }
    }
}
