package com.nextup.core.domain.game

import com.nextup.common.exception.DuplicatePlayerInLineupException
import com.nextup.common.exception.InvalidDhRuleException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.common.exception.MercenaryQuotaExceededException
import com.nextup.common.exception.NoCatcherInLineupException
import com.nextup.common.exception.NonAttendingPlayerInLineupException
import com.nextup.common.exception.UnregisteredPlayerInLineupException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class LineupValidatorTest {
    @Test
    fun `should pass validation with valid lineup`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should throw NoCatcherInLineupException when no catcher in starters`() {
        // given
        val submission = createLineupSubmission()
        val entries = createLineupWithoutCatcher(submission)

        // when & then
        assertThrows<NoCatcherInLineupException> {
            LineupValidator.validate(entries)
        }
    }

    @Test
    fun `should throw DuplicatePlayerInLineupException when same player appears twice`() {
        // given
        val submission = createLineupSubmission()
        val samePlayer = createPlayer("홍길동", Position.FIRST_BASE, 1L)
        val entries =
            listOf(
                createEntry(submission, samePlayer, Position.FIRST_BASE, 3, true),
                createEntry(submission, samePlayer, Position.THIRD_BASE, 5, true),
            )

        // when & then
        assertThrows<DuplicatePlayerInLineupException> {
            LineupValidator.validate(entries)
        }
    }

    @Test
    fun `should throw InvalidDhRuleException when DH and pitcher both have batting order`() {
        // given
        val submission = createLineupSubmission()
        val entries = createLineupWithDhAndPitcherBatting(submission)

        // when & then
        assertThrows<InvalidDhRuleException> {
            LineupValidator.validate(entries)
        }
    }

    @Test
    fun `should pass when DH exists and pitcher has no batting order`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineupWithDh(submission)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should pass when no DH and pitcher bats`() {
        // given
        val submission = createLineupSubmission()
        val entries = createLineupWithPitcherBatting(submission)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should allow non-starter duplicate check to include substitutes`() {
        // given
        val submission = createLineupSubmission()
        val player = createPlayer("김대기", Position.LEFT_FIELD, 10L)
        val entries =
            listOf(
                createEntry(
                    submission,
                    createPlayer("투수", Position.STARTING_PITCHER, 1L),
                    Position.STARTING_PITCHER,
                    1,
                    true
                ),
                createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
                createEntry(submission, player, Position.LEFT_FIELD, 3, true),
                createEntry(submission, player, Position.RIGHT_FIELD, null, false),
            )

        // when & then - same player as starter AND substitute is a duplicate
        assertThrows<DuplicatePlayerInLineupException> {
            LineupValidator.validate(entries)
        }
    }

    @Test
    fun `should pass when all players are attending`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val attendingPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries, attendingPlayerIds)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should throw NonAttendingPlayerInLineupException when non-attending player in lineup`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val attendingPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)

        // when & then
        val exception =
            assertThrows<NonAttendingPlayerInLineupException> {
                LineupValidator.validate(entries, attendingPlayerIds)
            }
        assertThatCode {
            exception.message?.contains("9")
        }
    }

    @Test
    fun `should throw NonAttendingPlayerInLineupException when multiple non-attending players`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val attendingPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L)

        // when & then
        assertThrows<NonAttendingPlayerInLineupException> {
            LineupValidator.validate(entries, attendingPlayerIds)
        }
    }

    @Test
    fun `should skip attendance validation when attendingPlayerIds is null`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)

        // when & then - should pass even if we don't provide attendance info
        assertThatCode {
            LineupValidator.validate(entries, null)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should validate attendance for both starters and substitutes`() {
        // given
        val submission = createLineupSubmission()
        val entries =
            listOf(
                createEntry(
                    submission,
                    createPlayer("투수", Position.STARTING_PITCHER, 1L),
                    Position.STARTING_PITCHER,
                    1,
                    true
                ),
                createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
                createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 3, true),
                createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 4, true),
                createEntry(submission, createPlayer("3루수", Position.THIRD_BASE, 5L), Position.THIRD_BASE, 5, true),
                createEntry(submission, createPlayer("유격수", Position.SHORTSTOP, 6L), Position.SHORTSTOP, 6, true),
                createEntry(submission, createPlayer("좌익수", Position.LEFT_FIELD, 7L), Position.LEFT_FIELD, 7, true),
                createEntry(submission, createPlayer("중견수", Position.CENTER_FIELD, 8L), Position.CENTER_FIELD, 8, true),
                createEntry(submission, createPlayer("우익수", Position.RIGHT_FIELD, 9L), Position.RIGHT_FIELD, 9, true),
                createEntry(
                    submission,
                    createPlayer("대기선수", Position.LEFT_FIELD, 10L),
                    Position.LEFT_FIELD,
                    null,
                    false
                ),
            )
        val attendingPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)

        // when & then - substitute (player 10) is not attending
        assertThrows<NonAttendingPlayerInLineupException> {
            LineupValidator.validate(entries, attendingPlayerIds)
        }
    }

    // ========== League Registration (부정선수 체크) Tests ==========

    @Test
    fun `should pass when all lineup players are registered for competition`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val registeredPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries, registeredPlayerIds = registeredPlayerIds)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should throw UnregisteredPlayerInLineupException when unregistered player in lineup`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        // player 9 (우익수) is not registered
        val registeredPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)

        // when & then
        val exception =
            assertThrows<UnregisteredPlayerInLineupException> {
                LineupValidator.validate(entries, registeredPlayerIds = registeredPlayerIds)
            }
        assert(exception.message?.contains("9") == true)
    }

    @Test
    fun `should throw UnregisteredPlayerInLineupException when multiple unregistered players`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        // players 7, 8, 9 are not registered
        val registeredPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L)

        // when & then
        assertThrows<UnregisteredPlayerInLineupException> {
            LineupValidator.validate(entries, registeredPlayerIds = registeredPlayerIds)
        }
    }

    @Test
    fun `should skip registration validation when registeredPlayerIds is null`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)

        // when & then - should pass even without registration info
        assertThatCode {
            LineupValidator.validate(entries, registeredPlayerIds = null)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should validate registration for both starters and substitutes`() {
        // given
        val submission = createLineupSubmission()
        val entries =
            listOf(
                createEntry(
                    submission,
                    createPlayer("투수", Position.STARTING_PITCHER, 1L),
                    Position.STARTING_PITCHER,
                    1,
                    true,
                ),
                createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
                createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 3, true),
                createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 4, true),
                createEntry(submission, createPlayer("3루수", Position.THIRD_BASE, 5L), Position.THIRD_BASE, 5, true),
                createEntry(submission, createPlayer("유격수", Position.SHORTSTOP, 6L), Position.SHORTSTOP, 6, true),
                createEntry(submission, createPlayer("좌익수", Position.LEFT_FIELD, 7L), Position.LEFT_FIELD, 7, true),
                createEntry(submission, createPlayer("중견수", Position.CENTER_FIELD, 8L), Position.CENTER_FIELD, 8, true),
                createEntry(submission, createPlayer("우익수", Position.RIGHT_FIELD, 9L), Position.RIGHT_FIELD, 9, true),
                createEntry(
                    submission,
                    createPlayer("대타", Position.LEFT_FIELD, 10L),
                    Position.LEFT_FIELD,
                    null,
                    false,
                ),
            )
        // substitute player 10 is not registered
        val registeredPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)

        // when & then
        assertThrows<UnregisteredPlayerInLineupException> {
            LineupValidator.validate(entries, registeredPlayerIds = registeredPlayerIds)
        }
    }

    @Test
    fun `should validate both attendance and registration when both provided`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val attendingPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)
        val registeredPlayerIds = setOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)

        // when & then - both validations pass
        assertThatCode {
            LineupValidator.validate(
                entries,
                attendingPlayerIds = attendingPlayerIds,
                registeredPlayerIds = registeredPlayerIds,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `validateLeagueRegisteredPlayers should throw for unregistered player`() {
        // given
        val submission = createLineupSubmission()
        val entries =
            listOf(
                createEntry(
                    submission,
                    createPlayer("투수", Position.STARTING_PITCHER, 1L),
                    Position.STARTING_PITCHER,
                    1,
                    true,
                ),
                createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
                // player 99 is not registered
                createEntry(
                    submission,
                    createPlayer("미등록선수", Position.LEFT_FIELD, 99L),
                    Position.LEFT_FIELD,
                    3,
                    true,
                ),
            )
        val registeredPlayerIds = setOf(1L, 2L)

        // when & then
        val exception =
            assertThrows<UnregisteredPlayerInLineupException> {
                LineupValidator.validateLeagueRegisteredPlayers(entries, registeredPlayerIds)
            }
        assert(exception.message?.contains("99") == true)
    }

    @Test
    fun `validateLeagueRegisteredPlayers should pass when all players registered`() {
        // given
        val submission = createLineupSubmission()
        val entries =
            listOf(
                createEntry(
                    submission,
                    createPlayer("투수", Position.STARTING_PITCHER, 1L),
                    Position.STARTING_PITCHER,
                    1,
                    true,
                ),
                createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
            )
        val registeredPlayerIds = setOf(1L, 2L)

        // when & then
        assertThatCode {
            LineupValidator.validateLeagueRegisteredPlayers(entries, registeredPlayerIds)
        }.doesNotThrowAnyException()
    }

    // ========== Post-DH Release Batting Order Validation Tests ==========

    @Test
    fun `should pass post-DH release validation with 9 batters in order`() {
        // given: DH 해제 후 투수 포함 9명이 타순에 있는 상태
        val gameTeam = mockk<GameTeam>(relaxed = true)
        val players =
            (1..8).map { order ->
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = createPlayer("야수$order", Position.CATCHER, order.toLong()),
                    position = Position.CATCHER,
                    battingOrder = order,
                )
            } +
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = createPlayer("투수", Position.STARTING_PITCHER, 9L),
                    position = Position.STARTING_PITCHER,
                    battingOrder = 9,
                )

        // when & then
        assertThatCode {
            LineupValidator.validatePostDhReleaseBattingOrder(players)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should fail post-DH release validation with 8 batters in order`() {
        // given: DH 해제 후 투수가 타순을 할당받지 못해 8명만 타순에 있는 상태
        val gameTeam = mockk<GameTeam>(relaxed = true)
        val players =
            (1..8).map { order ->
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = createPlayer("야수$order", Position.CATCHER, order.toLong()),
                    position = Position.CATCHER,
                    battingOrder = order,
                )
            } +
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = createPlayer("투수", Position.STARTING_PITCHER, 9L),
                    position = Position.STARTING_PITCHER,
                    battingOrder = null, // 투수에게 타순이 할당되지 않음
                )

        // when & then
        val exception =
            assertThrows<InvalidGameStateException> {
                LineupValidator.validatePostDhReleaseBattingOrder(players)
            }
        assert(exception.message?.contains("8명") == true)
    }

    @Test
    fun `should fail post-DH release validation with 10 batters in order`() {
        // given: 잘못된 상태로 10명이 타순에 있는 경우
        val gameTeam = mockk<GameTeam>(relaxed = true)
        val players =
            (1..10).map { order ->
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = createPlayer("선수$order", Position.CATCHER, order.toLong()),
                    position = Position.CATCHER,
                    battingOrder = order,
                )
            }

        // when & then
        val exception =
            assertThrows<InvalidGameStateException> {
                LineupValidator.validatePostDhReleaseBattingOrder(players)
            }
        assert(exception.message?.contains("10명") == true)
    }

    @Test
    fun `should only count currently playing players in post-DH release validation`() {
        // given: 퇴장한 선수는 카운트에서 제외
        val gameTeam = mockk<GameTeam>(relaxed = true)
        val activePlayers =
            (1..9).map { order ->
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = createPlayer("야수$order", Position.CATCHER, order.toLong()),
                    position = Position.CATCHER,
                    battingOrder = order,
                )
            }
        val exitedPlayer =
            GamePlayer.createStarter(
                gameTeam = gameTeam,
                player = createPlayer("퇴장선수", Position.LEFT_FIELD, 10L),
                position = Position.LEFT_FIELD,
                battingOrder = 5,
            )
        exitedPlayer.exitGame(3)

        // when & then: 퇴장한 선수(isCurrentlyPlaying=false)는 제외되므로 9명으로 통과
        assertThatCode {
            LineupValidator.validatePostDhReleaseBattingOrder(activePlayers + exitedPlayer)
        }.doesNotThrowAnyException()
    }

    // ========== L-3: Mercenary Quota Tests ==========

    @Test
    fun `should pass mercenary quota validation when mercenary count is within limit`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val mercenaryPlayerIds = setOf(1L, 2L) // 2명이 용병

        // when & then
        assertThatCode {
            LineupValidator.validate(
                entries,
                mercenaryPlayerIds = mercenaryPlayerIds,
                maxMercenaryCount = 3,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should throw MercenaryQuotaExceededException when mercenary count exceeds limit`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val mercenaryPlayerIds = setOf(1L, 2L, 3L) // 3명이 용병

        // when & then
        assertThrows<MercenaryQuotaExceededException> {
            LineupValidator.validate(
                entries,
                mercenaryPlayerIds = mercenaryPlayerIds,
                maxMercenaryCount = 2,
            )
        }
    }

    @Test
    fun `should skip mercenary validation when mercenaryPlayerIds is null`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)

        // when & then - should pass even without mercenary info
        assertThatCode {
            LineupValidator.validate(
                entries,
                mercenaryPlayerIds = null,
                maxMercenaryCount = 2,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should skip mercenary validation when maxMercenaryCount is null`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val mercenaryPlayerIds = setOf(1L, 2L, 3L, 4L, 5L) // 5명이 용병

        // when & then - should pass because maxMercenaryCount is null (unlimited)
        assertThatCode {
            LineupValidator.validate(
                entries,
                mercenaryPlayerIds = mercenaryPlayerIds,
                maxMercenaryCount = null,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should pass mercenary quota when count equals max`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val mercenaryPlayerIds = setOf(1L, 2L) // 정확히 2명이 용병

        // when & then - 정확히 한도와 같으면 통과
        assertThatCode {
            LineupValidator.validate(
                entries,
                mercenaryPlayerIds = mercenaryPlayerIds,
                maxMercenaryCount = 2,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should pass mercenary quota when maxMercenaryCount is 0 and no mercenaries`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)
        val mercenaryPlayerIds = setOf(100L, 200L) // 라인업에 없는 용병들

        // when & then
        assertThatCode {
            LineupValidator.validate(
                entries,
                mercenaryPlayerIds = mercenaryPlayerIds,
                maxMercenaryCount = 0,
            )
        }.doesNotThrowAnyException()
    }

    // ========== L-3: Mercenary Quota Substitution Tests ==========

    @Test
    fun `should pass substitution mercenary quota when adding non-mercenary`() {
        // given: 현재 용병 2명, 최대 2명, 비용병 투입
        // when & then
        assertThatCode {
            LineupValidator.validateMercenaryQuotaForSubstitution(
                currentMercenaryCount = 2,
                isIncomingPlayerMercenary = false,
                isOutgoingPlayerMercenary = false,
                maxMercenaryCount = 2,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should fail substitution mercenary quota when adding mercenary exceeds limit`() {
        // given: 현재 용병 2명, 최대 2명, 용병 투입 + 비용병 퇴장
        // when & then
        assertThrows<MercenaryQuotaExceededException> {
            LineupValidator.validateMercenaryQuotaForSubstitution(
                currentMercenaryCount = 2,
                isIncomingPlayerMercenary = true,
                isOutgoingPlayerMercenary = false,
                maxMercenaryCount = 2,
            )
        }
    }

    @Test
    fun `should pass substitution when replacing mercenary with mercenary`() {
        // given: 현재 용병 2명, 최대 2명, 용병 교체 (용병→용병)
        // when & then
        assertThatCode {
            LineupValidator.validateMercenaryQuotaForSubstitution(
                currentMercenaryCount = 2,
                isIncomingPlayerMercenary = true,
                isOutgoingPlayerMercenary = true,
                maxMercenaryCount = 2,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should pass substitution when removing mercenary frees up quota`() {
        // given: 현재 용병 2명, 최대 2명, 용병 퇴장 + 비용병 투입
        // when & then
        assertThatCode {
            LineupValidator.validateMercenaryQuotaForSubstitution(
                currentMercenaryCount = 2,
                isIncomingPlayerMercenary = false,
                isOutgoingPlayerMercenary = true,
                maxMercenaryCount = 2,
            )
        }.doesNotThrowAnyException()
    }

    // ========== 8인 라인업 테스트 (사회인 야구 minBattingOrderCount=8) ==========

    @Test
    fun `should pass validation with 8-person lineup when requiredBattingOrderCount is 8`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValid8PersonLineup(submission)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries, requiredBattingOrderCount = 8)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should fail validation with 8-person lineup when requiredBattingOrderCount is 9`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValid8PersonLineup(submission)

        // when & then
        assertThrows<com.nextup.common.exception.InvalidLineupBattingOrderCountException> {
            LineupValidator.validate(entries, requiredBattingOrderCount = 9)
        }
    }

    @Test
    fun `should fail validation with 9-person lineup when requiredBattingOrderCount is 8`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)

        // when & then
        assertThrows<com.nextup.common.exception.InvalidLineupBattingOrderCountException> {
            LineupValidator.validate(entries, requiredBattingOrderCount = 8)
        }
    }

    @Test
    fun `should pass validation with 9-person lineup when default requiredBattingOrderCount`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)

        // when & then - default is 9
        assertThatCode {
            LineupValidator.validate(entries)
        }.doesNotThrowAnyException()
    }

    // ========== Helper Methods ==========

    private fun createValid8PersonLineup(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                1,
                true,
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
            createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 3, true),
            createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 4, true),
            createEntry(submission, createPlayer("3루수", Position.THIRD_BASE, 5L), Position.THIRD_BASE, 5, true),
            createEntry(submission, createPlayer("유격수", Position.SHORTSTOP, 6L), Position.SHORTSTOP, 6, true),
            createEntry(submission, createPlayer("좌익수", Position.LEFT_FIELD, 7L), Position.LEFT_FIELD, 7, true),
            createEntry(submission, createPlayer("중견수", Position.CENTER_FIELD, 8L), Position.CENTER_FIELD, 8, true),
        )

    private fun createValidLineup(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                1,
                true
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
            createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 3, true),
            createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 4, true),
            createEntry(submission, createPlayer("3루수", Position.THIRD_BASE, 5L), Position.THIRD_BASE, 5, true),
            createEntry(submission, createPlayer("유격수", Position.SHORTSTOP, 6L), Position.SHORTSTOP, 6, true),
            createEntry(submission, createPlayer("좌익수", Position.LEFT_FIELD, 7L), Position.LEFT_FIELD, 7, true),
            createEntry(submission, createPlayer("중견수", Position.CENTER_FIELD, 8L), Position.CENTER_FIELD, 8, true),
            createEntry(submission, createPlayer("우익수", Position.RIGHT_FIELD, 9L), Position.RIGHT_FIELD, 9, true),
        )

    private fun createLineupWithoutCatcher(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                1,
                true
            ),
            createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 2, true),
            createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 3, true),
        )

    private fun createLineupWithDhAndPitcherBatting(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                1,
                true
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
            createEntry(
                submission,
                createPlayer("DH", Position.DESIGNATED_HITTER, 10L),
                Position.DESIGNATED_HITTER,
                3,
                true
            ),
        )

    private fun createValidLineupWithDh(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                null,
                true
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
            createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 3, true),
            createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 4, true),
            createEntry(submission, createPlayer("3루수", Position.THIRD_BASE, 5L), Position.THIRD_BASE, 5, true),
            createEntry(submission, createPlayer("유격수", Position.SHORTSTOP, 6L), Position.SHORTSTOP, 6, true),
            createEntry(submission, createPlayer("좌익수", Position.LEFT_FIELD, 7L), Position.LEFT_FIELD, 7, true),
            createEntry(submission, createPlayer("중견수", Position.CENTER_FIELD, 8L), Position.CENTER_FIELD, 8, true),
            createEntry(submission, createPlayer("우익수", Position.RIGHT_FIELD, 9L), Position.RIGHT_FIELD, 9, true),
            createEntry(
                submission,
                createPlayer("DH", Position.DESIGNATED_HITTER, 10L),
                Position.DESIGNATED_HITTER,
                1,
                true
            ),
        )

    private fun createLineupWithPitcherBatting(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                9,
                true
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 1, true),
            createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 2, true),
            createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 3, true),
            createEntry(submission, createPlayer("3루수", Position.THIRD_BASE, 5L), Position.THIRD_BASE, 4, true),
            createEntry(submission, createPlayer("유격수", Position.SHORTSTOP, 6L), Position.SHORTSTOP, 5, true),
            createEntry(submission, createPlayer("좌익수", Position.LEFT_FIELD, 7L), Position.LEFT_FIELD, 6, true),
            createEntry(submission, createPlayer("중견수", Position.CENTER_FIELD, 8L), Position.CENTER_FIELD, 7, true),
            createEntry(submission, createPlayer("우익수", Position.RIGHT_FIELD, 9L), Position.RIGHT_FIELD, 8, true),
        )

    private fun createEntry(
        submission: LineupSubmission,
        player: Player,
        position: Position,
        battingOrder: Int?,
        isStarter: Boolean,
    ): LineupEntry =
        LineupEntry(
            submission = submission,
            player = player,
            position = position,
            battingOrder = battingOrder,
            backNumber = null,
            isStarter = isStarter,
        )

    private fun createPlayer(
        name: String,
        position: Position,
        id: Long,
    ): Player =
        Player(
            name = name,
            primaryPosition = position,
            id = id,
        )

    private fun createLineupSubmission(): LineupSubmission {
        val association = Association(name = "서울시야구협회", abbreviation = "서야협", region = "서울")
        val league = League(association = association, name = "서울시 리그", foundedYear = 2020)
        val competition =
            Competition(
                league = league,
                name = "2024 시즌",
                year = 2024,
                startDate = LocalDate.now().minusDays(30),
            )
        val team = Team(league = league, name = "Tigers", city = "서울", foundedYear = 2020)
        val awayTeam = Team(league = league, name = "Eagles", city = "부산", foundedYear = 2020)
        val game =
            Game.createForTest(
                competition = competition,
                homeTeam = team,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.now().plusDays(1),
                location = "서울야구장",
            )
        val user = User.createLocalUser(email = "manager@test.com", encodedPassword = "encoded", nickname = "감독")
        return LineupSubmission.create(game, team, user)
    }
}
