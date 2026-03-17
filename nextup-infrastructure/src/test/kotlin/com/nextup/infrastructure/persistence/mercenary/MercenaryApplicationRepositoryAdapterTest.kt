package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryApplication
import com.nextup.core.domain.mercenary.MercenaryApplicationStatus
import com.nextup.core.domain.player.Position
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

@DisplayName("MercenaryApplicationRepositoryAdapter 테스트")
class MercenaryApplicationRepositoryAdapterTest {
    private lateinit var jpaRepository: MercenaryApplicationJpaRepository
    private lateinit var adapter: MercenaryApplicationRepositoryAdapter

    private lateinit var application: MercenaryApplication

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = MercenaryApplicationRepositoryAdapter(jpaRepository)

        application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 50L,
                preferredPositions = setOf(Position.STARTING_PITCHER),
                message = "열심히 하겠습니다",
            )
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        fun `용병 지원을 저장한다`() {
            // given
            every { jpaRepository.save(application) } returns application

            // when
            val result = adapter.save(application)

            // then
            assertThat(result).isEqualTo(application)
            verify { jpaRepository.save(application) }
        }
    }

    @Nested
    @DisplayName("findByIdOrNull")
    inner class FindByIdOrNull {
        @Test
        fun `ID로 용병 지원을 조회한다`() {
            // given
            every { jpaRepository.findByIdOrNull(1L) } returns application

            // when
            val result = adapter.findByIdOrNull(1L)

            // then
            assertThat(result).isEqualTo(application)
            verify { jpaRepository.findByIdOrNull(1L) }
        }

        @Test
        fun `존재하지 않는 ID로 조회하면 null을 반환한다`() {
            // given
            every { jpaRepository.findByIdOrNull(999L) } returns null

            // when
            val result = adapter.findByIdOrNull(999L)

            // then
            assertThat(result).isNull()
            verify { jpaRepository.findByIdOrNull(999L) }
        }
    }

    @Nested
    @DisplayName("findByRequestId")
    inner class FindByRequestId {
        @Test
        fun `용병 요청 ID로 지원 목록을 조회한다`() {
            // given
            every { jpaRepository.findByRequestId(1L) } returns listOf(application)

            // when
            val result = adapter.findByRequestId(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(application)
            verify { jpaRepository.findByRequestId(1L) }
        }

        @Test
        fun `지원이 없으면 빈 목록을 반환한다`() {
            // given
            every { jpaRepository.findByRequestId(999L) } returns emptyList()

            // when
            val result = adapter.findByRequestId(999L)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByRequestId(999L) }
        }
    }

    @Nested
    @DisplayName("findByPlayerId")
    inner class FindByPlayerId {
        @Test
        fun `선수 ID로 지원 목록을 조회한다`() {
            // given
            every { jpaRepository.findByPlayerId(50L) } returns listOf(application)

            // when
            val result = adapter.findByPlayerId(50L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(application)
            verify { jpaRepository.findByPlayerId(50L) }
        }

        @Test
        fun `지원이 없으면 빈 목록을 반환한다`() {
            // given
            every { jpaRepository.findByPlayerId(999L) } returns emptyList()

            // when
            val result = adapter.findByPlayerId(999L)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByPlayerId(999L) }
        }
    }

    @Nested
    @DisplayName("existsByRequestIdAndPlayerId")
    inner class ExistsByRequestIdAndPlayerId {
        @Test
        fun `이미 지원한 경우 true를 반환한다`() {
            // given
            every { jpaRepository.existsByRequestIdAndPlayerId(1L, 50L) } returns true

            // when
            val result = adapter.existsByRequestIdAndPlayerId(1L, 50L)

            // then
            assertThat(result).isTrue()
            verify { jpaRepository.existsByRequestIdAndPlayerId(1L, 50L) }
        }

        @Test
        fun `지원하지 않은 경우 false를 반환한다`() {
            // given
            every { jpaRepository.existsByRequestIdAndPlayerId(1L, 999L) } returns false

            // when
            val result = adapter.existsByRequestIdAndPlayerId(1L, 999L)

            // then
            assertThat(result).isFalse()
            verify { jpaRepository.existsByRequestIdAndPlayerId(1L, 999L) }
        }
    }

    @Nested
    @DisplayName("countAcceptedByRequestId")
    inner class CountAcceptedByRequestId {
        @Test
        fun `용병 요청의 수락된 지원 수를 반환한다`() {
            // given
            every {
                jpaRepository.countByRequestIdAndStatus(1L, MercenaryApplicationStatus.ACCEPTED)
            } returns 2L

            // when
            val result = adapter.countAcceptedByRequestId(1L)

            // then
            assertThat(result).isEqualTo(2L)
            verify { jpaRepository.countByRequestIdAndStatus(1L, MercenaryApplicationStatus.ACCEPTED) }
        }

        @Test
        fun `수락된 지원이 없으면 0을 반환한다`() {
            // given
            every {
                jpaRepository.countByRequestIdAndStatus(1L, MercenaryApplicationStatus.ACCEPTED)
            } returns 0L

            // when
            val result = adapter.countAcceptedByRequestId(1L)

            // then
            assertThat(result).isEqualTo(0L)
            verify { jpaRepository.countByRequestIdAndStatus(1L, MercenaryApplicationStatus.ACCEPTED) }
        }
    }
}
