package com.nextup.core.domain.league

import com.nextup.core.domain.association.Association
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("League 엔티티 테스트")
class LeagueTest {
    private lateinit var association: Association

    @BeforeEach
    fun setUp() {
        association =
            Association(
                name = "서울시야구협회",
                abbreviation = "SBA",
                region = "서울",
            )
    }

    private fun createLeague(
        isActive: Boolean = true,
        maxTeamCount: Int? = null,
    ): League =
        League(
            association = association,
            name = "1부 리그",
            abbreviation = "1st",
            foundedYear = 2020,
            divisionLevel = 1,
            description = "최상위 리그",
            logoUrl = "https://example.com/logo.png",
            isActive = isActive,
            maxTeamCount = maxTeamCount,
        )

    @Nested
    @DisplayName("활성화/비활성화")
    inner class ActivationTests {
        @Test
        fun `리그를 비활성화할 수 있다`() {
            // given
            val league = createLeague(isActive = true)

            // when
            league.deactivate()

            // then
            assertThat(league.isActive).isFalse()
        }

        @Test
        fun `리그를 활성화할 수 있다`() {
            // given
            val league = createLeague(isActive = false)

            // when
            league.activate()

            // then
            assertThat(league.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("정보 업데이트")
    inner class UpdateInfoTests {
        @Test
        fun `설명을 업데이트할 수 있다`() {
            // given
            val league = createLeague()
            val newDescription = "수정된 설명"

            // when
            league.updateInfo(description = newDescription)

            // then
            assertThat(league.description).isEqualTo(newDescription)
        }

        @Test
        fun `로고 URL을 업데이트할 수 있다`() {
            // given
            val league = createLeague()
            val newLogoUrl = "https://example.com/new-logo.png"

            // when
            league.updateInfo(logoUrl = newLogoUrl)

            // then
            assertThat(league.logoUrl).isEqualTo(newLogoUrl)
        }

        @Test
        fun `설명과 로고 URL을 동시에 업데이트할 수 있다`() {
            // given
            val league = createLeague()

            // when
            league.updateInfo(
                description = "새로운 설명",
                logoUrl = "https://example.com/updated.png",
            )

            // then
            assertThat(league.description).isEqualTo("새로운 설명")
            assertThat(league.logoUrl).isEqualTo("https://example.com/updated.png")
        }
    }

    @Nested
    @DisplayName("기본 속성")
    inner class PropertyTests {
        @Test
        fun `리그 생성 시 기본값으로 활성 상태이다`() {
            // given & when
            val league =
                League(
                    association = association,
                    name = "테스트 리그",
                    foundedYear = 2025,
                    divisionLevel = 1,
                )

            // then
            assertThat(league.isActive).isTrue()
        }

        @Test
        fun `리그 속성이 올바르게 설정된다`() {
            // given & when
            val league = createLeague()

            // then
            assertThat(league.name).isEqualTo("1부 리그")
            assertThat(league.abbreviation).isEqualTo("1st")
            assertThat(league.foundedYear).isEqualTo(2020)
            assertThat(league.divisionLevel).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("팀 수용 가능 여부")
    inner class CanAcceptTeamTests {
        @Test
        fun `최대 팀 수 미만이면 팀을 수용할 수 있다`() {
            // given
            val league = createLeague(maxTeamCount = 10)

            // when
            val result = league.canAcceptTeam(currentTeamCount = 9)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `현재 팀 수가 최대 팀 수에 도달하면 팀을 수용할 수 없다`() {
            // given
            val league = createLeague(maxTeamCount = 10)

            // when
            val result = league.canAcceptTeam(currentTeamCount = 10)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `최대 팀 수가 설정되지 않으면 제한 없이 팀을 수용할 수 있다`() {
            // given
            val league = createLeague(maxTeamCount = null)

            // when
            val result = league.canAcceptTeam(currentTeamCount = 100)

            // then
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("등록 기간 확인")
    inner class IsRegistrationOpenTests {
        @Test
        fun `등록 기간 내이고 리그가 활성화되어 있으면 등록이 열려 있다`() {
            // given
            val league = createLeague(isActive = true)
            val today = LocalDate.of(2025, 3, 15)

            // when
            val result =
                league.isRegistrationOpen(
                    registrationStartDate = LocalDate.of(2025, 3, 1),
                    registrationEndDate = LocalDate.of(2025, 3, 31),
                    today = today,
                )

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `등록 기간이 지나면 등록이 닫혀 있다`() {
            // given
            val league = createLeague(isActive = true)
            val today = LocalDate.of(2025, 4, 1)

            // when
            val result =
                league.isRegistrationOpen(
                    registrationStartDate = LocalDate.of(2025, 3, 1),
                    registrationEndDate = LocalDate.of(2025, 3, 31),
                    today = today,
                )

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `등록 시작일 이전이면 등록이 닫혀 있다`() {
            // given
            val league = createLeague(isActive = true)
            val today = LocalDate.of(2025, 2, 28)

            // when
            val result =
                league.isRegistrationOpen(
                    registrationStartDate = LocalDate.of(2025, 3, 1),
                    registrationEndDate = LocalDate.of(2025, 3, 31),
                    today = today,
                )

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `리그가 비활성화되어 있으면 등록이 닫혀 있다`() {
            // given
            val league = createLeague(isActive = false)
            val today = LocalDate.of(2025, 3, 15)

            // when
            val result =
                league.isRegistrationOpen(
                    registrationStartDate = LocalDate.of(2025, 3, 1),
                    registrationEndDate = LocalDate.of(2025, 3, 31),
                    today = today,
                )

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("시즌 날짜 유효성 검증")
    inner class ValidateSeasonDatesTests {
        @Test
        fun `시작일이 종료일보다 이전이면 유효하다`() {
            // given
            val league = createLeague()

            // when & then (예외 발생 없음)
            league.validateSeasonDates(
                startDate = LocalDate.of(2025, 4, 1),
                endDate = LocalDate.of(2025, 10, 31),
            )
        }

        @Test
        fun `시작일이 종료일과 같으면 예외가 발생한다`() {
            // given
            val league = createLeague()
            val sameDate = LocalDate.of(2025, 4, 1)

            // when & then
            assertThrows(IllegalArgumentException::class.java) {
                league.validateSeasonDates(
                    startDate = sameDate,
                    endDate = sameDate,
                )
            }
        }

        @Test
        fun `시작일이 종료일보다 이후이면 예외가 발생한다`() {
            // given
            val league = createLeague()

            // when & then
            assertThrows(IllegalArgumentException::class.java) {
                league.validateSeasonDates(
                    startDate = LocalDate.of(2025, 10, 31),
                    endDate = LocalDate.of(2025, 4, 1),
                )
            }
        }
    }
}
