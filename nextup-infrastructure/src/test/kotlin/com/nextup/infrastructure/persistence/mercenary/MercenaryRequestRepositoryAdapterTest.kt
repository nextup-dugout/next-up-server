package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryRequest
import com.nextup.core.domain.mercenary.MercenaryRequestStatus
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
import java.time.Instant

@DisplayName("MercenaryRequestRepositoryAdapter 테스트")
class MercenaryRequestRepositoryAdapterTest {
    private lateinit var jpaRepository: MercenaryRequestJpaRepository
    private lateinit var adapter: MercenaryRequestRepositoryAdapter

    private lateinit var mercenaryRequest: MercenaryRequest

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = MercenaryRequestRepositoryAdapter(jpaRepository)

        mercenaryRequest =
            MercenaryRequest.create(
                requestingTeamId = 10L,
                gameId = 100L,
                positions = setOf(Position.STARTING_PITCHER, Position.CATCHER),
                maxCount = 2,
                deadline = Instant.now().plusSeconds(86400),
                description = "투수 1명, 포수 1명 구합니다",
            )
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        fun `용병 요청을 저장한다`() {
            // given
            every { jpaRepository.save(mercenaryRequest) } returns mercenaryRequest

            // when
            val result = adapter.save(mercenaryRequest)

            // then
            assertThat(result).isEqualTo(mercenaryRequest)
            verify { jpaRepository.save(mercenaryRequest) }
        }
    }

    @Nested
    @DisplayName("findByIdOrNull")
    inner class FindByIdOrNull {
        @Test
        fun `ID로 용병 요청을 조회한다`() {
            // given
            every { jpaRepository.findByIdOrNull(1L) } returns mercenaryRequest

            // when
            val result = adapter.findByIdOrNull(1L)

            // then
            assertThat(result).isEqualTo(mercenaryRequest)
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
    @DisplayName("findByStatus")
    inner class FindByStatus {
        @Test
        fun `OPEN 상태의 용병 요청 목록을 조회한다`() {
            // given
            every { jpaRepository.findByStatus(MercenaryRequestStatus.OPEN) } returns listOf(mercenaryRequest)

            // when
            val result = adapter.findByStatus(MercenaryRequestStatus.OPEN)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(mercenaryRequest)
            verify { jpaRepository.findByStatus(MercenaryRequestStatus.OPEN) }
        }

        @Test
        fun `해당 상태의 요청이 없으면 빈 목록을 반환한다`() {
            // given
            every { jpaRepository.findByStatus(MercenaryRequestStatus.CLOSED) } returns emptyList()

            // when
            val result = adapter.findByStatus(MercenaryRequestStatus.CLOSED)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByStatus(MercenaryRequestStatus.CLOSED) }
        }

        @Test
        fun `CANCELLED 상태의 용병 요청 목록을 조회한다`() {
            // given
            every { jpaRepository.findByStatus(MercenaryRequestStatus.CANCELLED) } returns emptyList()

            // when
            val result = adapter.findByStatus(MercenaryRequestStatus.CANCELLED)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByStatus(MercenaryRequestStatus.CANCELLED) }
        }
    }

    @Nested
    @DisplayName("findByRequestingTeamId")
    inner class FindByRequestingTeamId {
        @Test
        fun `요청 팀 ID로 용병 요청 목록을 조회한다`() {
            // given
            every { jpaRepository.findByRequestingTeamId(10L) } returns listOf(mercenaryRequest)

            // when
            val result = adapter.findByRequestingTeamId(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(mercenaryRequest)
            verify { jpaRepository.findByRequestingTeamId(10L) }
        }

        @Test
        fun `해당 팀의 요청이 없으면 빈 목록을 반환한다`() {
            // given
            every { jpaRepository.findByRequestingTeamId(999L) } returns emptyList()

            // when
            val result = adapter.findByRequestingTeamId(999L)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByRequestingTeamId(999L) }
        }
    }

    @Nested
    @DisplayName("findByGameId")
    inner class FindByGameId {
        @Test
        fun `경기 ID로 용병 요청 목록을 조회한다`() {
            // given
            every { jpaRepository.findByGameId(100L) } returns listOf(mercenaryRequest)

            // when
            val result = adapter.findByGameId(100L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(mercenaryRequest)
            verify { jpaRepository.findByGameId(100L) }
        }

        @Test
        fun `해당 경기의 요청이 없으면 빈 목록을 반환한다`() {
            // given
            every { jpaRepository.findByGameId(999L) } returns emptyList()

            // when
            val result = adapter.findByGameId(999L)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByGameId(999L) }
        }
    }
}
