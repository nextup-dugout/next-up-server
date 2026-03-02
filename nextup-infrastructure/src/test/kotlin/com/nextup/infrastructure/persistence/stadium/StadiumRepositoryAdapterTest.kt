package com.nextup.infrastructure.persistence.stadium

import com.nextup.core.common.PageCommand
import com.nextup.core.domain.stadium.Stadium
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.repository.findByIdOrNull

@DisplayName("StadiumRepositoryAdapter")
class StadiumRepositoryAdapterTest {
    private lateinit var jpaRepository: StadiumJpaRepository
    private lateinit var adapter: StadiumRepositoryAdapter

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = StadiumRepositoryAdapter(jpaRepository)
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        fun `should save and return stadium`() {
            // given
            val stadium = createStadium("잠실 야구장", 37.5121, 127.0717)
            every { jpaRepository.save(stadium) } returns stadium

            // when
            val result = adapter.save(stadium)

            // then
            assertThat(result).isEqualTo(stadium)
            verify { jpaRepository.save(stadium) }
        }
    }

    @Nested
    @DisplayName("findByIdOrNull")
    inner class FindByIdOrNull {
        @Test
        fun `should return stadium when found`() {
            // given
            val stadium = createStadium("잠실 야구장", 37.5121, 127.0717)
            every { jpaRepository.findByIdOrNull(1L) } returns stadium

            // when
            val result = adapter.findByIdOrNull(1L)

            // then
            assertThat(result).isEqualTo(stadium)
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
    @DisplayName("findAll")
    inner class FindAll {
        @Test
        fun `should return all stadiums`() {
            // given
            val stadiums =
                listOf(
                    createStadium("잠실 야구장", 37.5121, 127.0717),
                    createStadium("고척 야구장", 37.4981, 126.8671),
                )
            every { jpaRepository.findAll() } returns stadiums

            // when
            val result = adapter.findAll()

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("findAllActive")
    inner class FindAllActive {
        @Test
        fun `should return only active stadiums`() {
            // given
            val activeStadium = createStadium("잠실 야구장", 37.5121, 127.0717)
            every { jpaRepository.findByIsActiveTrue() } returns listOf(activeStadium)

            // when
            val result = adapter.findAllActive()

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("findNearby")
    inner class FindNearby {
        @Test
        fun `should convert km to meters and call jpaRepository`() {
            // given
            val stadiums = listOf(createStadium("잠실 야구장", 37.5121, 127.0717))
            // 10km -> 10000.0 meters
            every { jpaRepository.findNearby(37.5, 127.0, 10000.0) } returns stadiums

            // when
            val result = adapter.findNearby(37.5, 127.0, 10.0)

            // then
            assertThat(result).hasSize(1)
            verify { jpaRepository.findNearby(37.5, 127.0, 10000.0) }
        }

        @Test
        fun `should convert 5km to 5000 meters`() {
            // given
            val stadiums = emptyList<Stadium>()
            every { jpaRepository.findNearby(35.0, 129.0, 5000.0) } returns stadiums

            // when
            val result = adapter.findNearby(35.0, 129.0, 5.0)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findNearby(35.0, 129.0, 5000.0) }
        }
    }

    @Nested
    @DisplayName("findNearbyStadiums")
    inner class FindNearbyStadiums {
        @Test
        fun `should convert km to meters and return paged result`() {
            // given
            val stadiums = listOf(createStadium("잠실 야구장", 37.5121, 127.0717))
            val pageCommand = PageCommand(page = 0, size = 20)
            val page = PageImpl(stadiums)
            // 10km -> 10000.0 meters
            every {
                jpaRepository.findNearbyOrderByDistance(37.5, 127.0, 10000.0, any())
            } returns page

            // when
            val result = adapter.findNearbyStadiums(37.5, 127.0, 10.0, pageCommand)

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.totalElements).isEqualTo(1)
            verify { jpaRepository.findNearbyOrderByDistance(37.5, 127.0, 10000.0, any()) }
        }

        @Test
        fun `should return empty page when no stadiums found`() {
            // given
            val pageCommand = PageCommand(page = 0, size = 20)
            val emptyPage = PageImpl(emptyList<Stadium>())
            every {
                jpaRepository.findNearbyOrderByDistance(37.5, 127.0, 1000.0, any())
            } returns emptyPage

            // when
            val result = adapter.findNearbyStadiums(37.5, 127.0, 1.0, pageCommand)

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }

    private fun createStadium(
        name: String,
        latitude: Double,
        longitude: Double,
    ): Stadium =
        Stadium.create(
            name = name,
            address = "서울특별시",
            latitude = latitude,
            longitude = longitude,
        )
}
