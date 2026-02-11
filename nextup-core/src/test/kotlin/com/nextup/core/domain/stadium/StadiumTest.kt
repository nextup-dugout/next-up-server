package com.nextup.core.domain.stadium

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Stadium")
class StadiumTest {
    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should create stadium successfully`() {
            // when
            val stadium =
                Stadium.create(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구 올림픽로 25",
                    latitude = 37.5121,
                    longitude = 127.0717,
                    capacity = 25000,
                    facilities = "주차장, 샤워실",
                    contactInfo = "02-1234-5678",
                )

            // then
            assertThat(stadium.name).isEqualTo("잠실 야구장")
            assertThat(stadium.latitude).isEqualTo(37.5121)
            assertThat(stadium.longitude).isEqualTo(127.0717)
            assertThat(stadium.isActive).isTrue()
        }

        @Test
        fun `should throw exception when name is blank`() {
            // when & then
            assertThatThrownBy {
                Stadium.create(
                    name = "",
                    address = "서울특별시 송파구",
                    latitude = 37.5121,
                    longitude = 127.0717,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Stadium name cannot be blank")
        }

        @Test
        fun `should throw exception when latitude is invalid`() {
            // when & then
            assertThatThrownBy {
                Stadium.create(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구",
                    latitude = 91.0,
                    longitude = 127.0717,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Latitude must be between -90 and 90")
        }

        @Test
        fun `should throw exception when longitude is invalid`() {
            // when & then
            assertThatThrownBy {
                Stadium.create(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구",
                    latitude = 37.5121,
                    longitude = 181.0,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Longitude must be between -180 and 180")
        }

        @Test
        fun `should throw exception when capacity is not positive`() {
            // when & then
            assertThatThrownBy {
                Stadium.create(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구",
                    latitude = 37.5121,
                    longitude = 127.0717,
                    capacity = -100,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Capacity must be positive")
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {
        @Test
        fun `should update stadium info successfully`() {
            // given
            val stadium =
                Stadium.create(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구",
                    latitude = 37.5121,
                    longitude = 127.0717,
                )

            // when
            stadium.update(
                address = "서울특별시 송파구 올림픽로 25",
                capacity = 25000,
                facilities = "주차장, 샤워실",
            )

            // then
            assertThat(stadium.address).isEqualTo("서울특별시 송파구 올림픽로 25")
            assertThat(stadium.capacity).isEqualTo(25000)
            assertThat(stadium.facilities).isEqualTo("주차장, 샤워실")
        }

        @Test
        fun `should throw exception when updating with invalid latitude`() {
            // given
            val stadium =
                Stadium.create(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구",
                    latitude = 37.5121,
                    longitude = 127.0717,
                )

            // when & then
            assertThatThrownBy {
                stadium.update(latitude = 95.0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Latitude must be between -90 and 90")
        }
    }

    @Nested
    @DisplayName("activate/deactivate")
    inner class ActivateDeactivate {
        @Test
        fun `should deactivate stadium`() {
            // given
            val stadium =
                Stadium.create(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구",
                    latitude = 37.5121,
                    longitude = 127.0717,
                )

            // when
            stadium.deactivate()

            // then
            assertThat(stadium.isActive).isFalse()
        }

        @Test
        fun `should activate stadium`() {
            // given
            val stadium =
                Stadium.create(
                    name = "잠실 야구장",
                    address = "서울특별시 송파구",
                    latitude = 37.5121,
                    longitude = 127.0717,
                )
            stadium.deactivate()

            // when
            stadium.activate()

            // then
            assertThat(stadium.isActive).isTrue()
        }
    }
}
