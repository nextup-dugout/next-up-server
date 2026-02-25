package com.nextup.core.service.stadium

import com.nextup.common.exception.StadiumNotFoundException
import com.nextup.core.domain.stadium.Stadium
import com.nextup.core.port.repository.StadiumRepositoryPort
import com.nextup.core.port.repository.StadiumSlotRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("StadiumAdminService - Extended")
class StadiumAdminServiceExtendedTest {
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
    @DisplayName("deactivateStadium")
    inner class DeactivateStadium {
        @Test
        fun `should throw exception when stadium not found`() {
            // given
            every { stadiumRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                stadiumAdminService.deactivateStadium(999L)
            }.isInstanceOf(StadiumNotFoundException::class.java)
        }

        @Test
        fun `should deactivate active stadium`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            assertThat(stadium.isActive).isTrue()
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
        fun `should throw exception when stadium not found`() {
            // given
            every { stadiumRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                stadiumAdminService.activateStadium(999L)
            }.isInstanceOf(StadiumNotFoundException::class.java)
        }

        @Test
        fun `should activate deactivated stadium`() {
            // given
            val stadium =
                createStadium(1L, "잠실 야구장", 37.5121, 127.0717).apply {
                    deactivate()
                }
            assertThat(stadium.isActive).isFalse()
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium

            // when
            val result = stadiumAdminService.activateStadium(1L)

            // then
            assertThat(result.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("updateStadium - additional cases")
    inner class UpdateStadium {
        @Test
        fun `should update latitude and longitude when provided`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium

            // when
            val result =
                stadiumAdminService.updateStadium(
                    id = 1L,
                    latitude = 37.6000,
                    longitude = 127.1000,
                )

            // then
            assertThat(result.latitude).isEqualTo(37.6000)
            assertThat(result.longitude).isEqualTo(127.1000)
        }

        @Test
        fun `should update contactInfo and imageUrls when provided`() {
            // given
            val stadium = createStadium(1L, "잠실 야구장", 37.5121, 127.0717)
            every { stadiumRepository.findByIdOrNull(1L) } returns stadium

            // when
            val result =
                stadiumAdminService.updateStadium(
                    id = 1L,
                    contactInfo = "02-9999-8888",
                    imageUrls = "https://example.com/img.jpg",
                )

            // then
            assertThat(result.contactInfo).isEqualTo("02-9999-8888")
            assertThat(result.imageUrls).isEqualTo("https://example.com/img.jpg")
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
