package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.domain.stadium.SlotStatus
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.domain.stadium.StadiumSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("StadiumSlotRepositoryAdapter")
class StadiumSlotRepositoryAdapterTest {
    private lateinit var jpaRepository: StadiumSlotJpaRepository
    private lateinit var adapter: StadiumSlotRepositoryAdapter

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = StadiumSlotRepositoryAdapter(jpaRepository)
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        fun `should save and return slot`() {
            // given
            val stadium = createStadium()
            val slot = createSlot(stadium, LocalDate.of(2024, 12, 25))
            every { jpaRepository.save(slot) } returns slot

            // when
            val result = adapter.save(slot)

            // then
            assertThat(result).isEqualTo(slot)
            verify { jpaRepository.save(slot) }
        }
    }

    @Nested
    @DisplayName("findByIdOrNull")
    inner class FindByIdOrNull {
        @Test
        fun `should return slot when found`() {
            // given
            val stadium = createStadium()
            val slot = createSlot(stadium, LocalDate.of(2024, 12, 25))
            every { jpaRepository.findByIdOrNull(1L) } returns slot

            // when
            val result = adapter.findByIdOrNull(1L)

            // then
            assertThat(result).isEqualTo(slot)
        }

        @Test
        fun `should return null when not found`() {
            // given
            every { jpaRepository.findByIdOrNull(999L) } returns null

            // when
            val result = adapter.findByIdOrNull(999L)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findByStadiumIdAndDate")
    inner class FindByStadiumIdAndDate {
        @Test
        fun `should return slots for given stadium and date`() {
            // given
            val stadium = createStadium()
            val date = LocalDate.of(2024, 12, 25)
            val slots =
                listOf(
                    createSlot(stadium, date),
                    createSlot(stadium, date),
                )
            every { jpaRepository.findByStadiumIdAndDate(1L, date) } returns slots

            // when
            val result = adapter.findByStadiumIdAndDate(1L, date)

            // then
            assertThat(result).hasSize(2)
        }

        @Test
        fun `should return empty list when no slots found`() {
            // given
            val date = LocalDate.of(2024, 12, 25)
            every { jpaRepository.findByStadiumIdAndDate(1L, date) } returns emptyList()

            // when
            val result = adapter.findByStadiumIdAndDate(1L, date)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findByStadiumIdAndDateBetween")
    inner class FindByStadiumIdAndDateBetween {
        @Test
        fun `should return slots within date range`() {
            // given
            val stadium = createStadium()
            val startDate = LocalDate.of(2024, 12, 1)
            val endDate = LocalDate.of(2024, 12, 31)
            val slots =
                listOf(
                    createSlot(stadium, LocalDate.of(2024, 12, 10)),
                    createSlot(stadium, LocalDate.of(2024, 12, 20)),
                )
            every {
                jpaRepository.findByStadiumIdAndDateBetween(1L, startDate, endDate)
            } returns slots

            // when
            val result = adapter.findByStadiumIdAndDateBetween(1L, startDate, endDate)

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("findByStadiumIdAndStatus")
    inner class FindByStadiumIdAndStatus {
        @Test
        fun `should return slots with given status`() {
            // given
            val stadium = createStadium()
            val slot = createSlot(stadium, LocalDate.of(2024, 12, 25))
            every {
                jpaRepository.findByStadiumIdAndStatus(1L, SlotStatus.AVAILABLE)
            } returns listOf(slot)

            // when
            val result = adapter.findByStadiumIdAndStatus(1L, SlotStatus.AVAILABLE)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].status).isEqualTo(SlotStatus.AVAILABLE)
        }

        @Test
        fun `should return empty list when no slots with given status`() {
            // given
            every {
                jpaRepository.findByStadiumIdAndStatus(1L, SlotStatus.BOOKED)
            } returns emptyList()

            // when
            val result = adapter.findByStadiumIdAndStatus(1L, SlotStatus.BOOKED)

            // then
            assertThat(result).isEmpty()
        }
    }

    private fun createStadium(): Stadium =
        Stadium.create(
            name = "잠실 야구장",
            address = "서울특별시",
            latitude = 37.5121,
            longitude = 127.0717,
        )

    private fun createSlot(
        stadium: Stadium,
        date: LocalDate,
    ): StadiumSlot =
        StadiumSlot.create(
            stadium = stadium,
            date = date,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(12, 0),
        )
}
