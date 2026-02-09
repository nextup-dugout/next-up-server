package com.nextup.core.service.stadium

import com.nextup.common.exception.StadiumNotFoundException
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.port.repository.StadiumRepositoryPort
import com.nextup.core.port.repository.StadiumSlotRepositoryPort
import com.nextup.core.service.stadium.dto.CreateSlotRequest
import com.nextup.core.service.stadium.dto.CreateStadiumRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("StadiumAdminService")
class StadiumAdminServiceTest {
    private lateinit var stadiumRepository: StadiumRepositoryPort
    private lateinit var slotRepository: StadiumSlotRepositoryPort
    private lateinit var stadiumAdminService: StadiumAdminService

    @BeforeEach
    fun setUp() {
        stadiumRepository = mockk()
        slotRepository = mockk()
        stadiumAdminService = StadiumAdminService(stadiumRepository, slotRepository)
    }

    @Nested
    @DisplayName("createStadium")
    inner class CreateStadium {
        @Test
        fun `should create stadium successfully`() {
            // given
            val request =
                CreateStadiumRequest(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구 올림픽로 25",
                    latitude = 37.5121,
                    longitude = 127.0717,
                    capacity = 25000,
                    facilities = "주차장, 샤워실",
                    contactInfo = "02-1234-5678",
                )
            every { stadiumRepository.save(any()) } answers { firstArg() }

            // when
            val result = stadiumAdminService.createStadium(request)

            // then
            assertThat(result.name).isEqualTo("잠실 야구장")
            assertThat(result.latitude).isEqualTo(37.5121)
            assertThat(result.isActive).isTrue()
            verify { stadiumRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("updateStadium")
    inner class UpdateStadium {
        @Test
        fun `should update stadium successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium

            // when
            val result =
                stadiumAdminService.updateStadium(
                    id = 1L,
                    address = "서울특별시 송파구 올림픽로 25",
                    capacity = 26000,
                    facilities = "주차장, 샤워실, 식당",
                )

            // then
            assertThat(result.address).isEqualTo("서울특별시 송파구 올림픽로 25")
            assertThat(result.capacity).isEqualTo(26000)
            assertThat(result.facilities).isEqualTo("주차장, 샤워실, 식당")
        }

        @Test
        fun `should throw exception when stadium not found`() {
            // given
            every { stadiumRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                stadiumAdminService.updateStadium(999L)
            }.isInstanceOf(StadiumNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("createSlots")
    inner class CreateSlots {
        @Test
        fun `should create multiple slots successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            val requests =
                listOf(
                    CreateSlotRequest(
                        stadiumId = 1L,
                        date = LocalDate.of(2024, 12, 25),
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(12, 0),
                        price = BigDecimal("50000"),
                    ),
                    CreateSlotRequest(
                        stadiumId = 1L,
                        date = LocalDate.of(2024, 12, 25),
                        startTime = LocalTime.of(14, 0),
                        endTime = LocalTime.of(16, 0),
                        price = BigDecimal("50000"),
                    ),
                )
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium
            every { slotRepository.save(any()) } answers { firstArg() }

            // when
            val result = stadiumAdminService.createSlots(requests)

            // then
            assertThat(result).hasSize(2)
            verify(exactly = 2) { slotRepository.save(any()) }
        }

        @Test
        fun `should throw exception when stadium not found`() {
            // given
            val requests =
                listOf(
                    CreateSlotRequest(
                        stadiumId = 999L,
                        date = LocalDate.of(2024, 12, 25),
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(12, 0),
                    ),
                )
            every { stadiumRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                stadiumAdminService.createSlots(requests)
            }.isInstanceOf(StadiumNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deactivateStadium")
    inner class DeactivateStadium {
        @Test
        fun `should deactivate stadium successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium

            // when
            val result = stadiumAdminService.deactivateStadium(1L)

            // then
            assertThat(result.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("activateStadium")
    inner class ActivateStadium {
        @Test
        fun `should activate stadium successfully`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717).apply { deactivate() }
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium

            // when
            val result = stadiumAdminService.activateStadium(1L)

            // then
            assertThat(result.isActive).isTrue()
        }
    }

    private fun createStadium(
        id: Long,
        name: String,
        latitude: Double,
        longitude: Double,
    ): Stadium {
        val stadium =
            Stadium.create(
                name = name,
                address = "서울특별시",
                latitude = latitude,
                longitude = longitude,
            )
        val idField = Stadium::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(stadium, id)
        return stadium
    }
}
