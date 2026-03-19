package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("CorrectionRequest 엔티티 테스트")
class CorrectionRequestTest {
    private fun createValidRequest(): CorrectionRequest =
        CorrectionRequest.create(
            gameId = 1L,
            requesterUserId = 10L,
            correctionType = CorrectionType.BATTING,
            targetRecordId = 100L,
            fieldName = "hits",
            newValue = "3",
            reason = "기록원 입력 오류",
        )

    @Nested
    @DisplayName("정정 요청 생성")
    inner class Create {
        @Test
        fun `유효한 파라미터로 정정 요청을 생성할 수 있다`() {
            // when
            val request = createValidRequest()

            // then
            assertThat(request.gameId).isEqualTo(1L)
            assertThat(request.requesterUserId).isEqualTo(10L)
            assertThat(request.correctionType).isEqualTo(CorrectionType.BATTING)
            assertThat(request.targetRecordId).isEqualTo(100L)
            assertThat(request.fieldName).isEqualTo("hits")
            assertThat(request.newValue).isEqualTo("3")
            assertThat(request.reason).isEqualTo("기록원 입력 오류")
            assertThat(request.status).isEqualTo(CorrectionRequestStatus.PENDING)
            assertThat(request.reviewerUserId).isNull()
            assertThat(request.reviewComment).isNull()
            assertThat(request.reviewedAt).isNull()
        }

        @Test
        fun `gameId가 0이면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                CorrectionRequest.create(
                    gameId = 0L,
                    requesterUserId = 10L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 100L,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "정정 사유",
                )
            }
        }

        @Test
        fun `gameId가 음수이면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                CorrectionRequest.create(
                    gameId = -1L,
                    requesterUserId = 10L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 100L,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "정정 사유",
                )
            }
        }

        @Test
        fun `requesterUserId가 0이면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                CorrectionRequest.create(
                    gameId = 1L,
                    requesterUserId = 0L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 100L,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "정정 사유",
                )
            }
        }

        @Test
        fun `fieldName이 빈 문자열이면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                CorrectionRequest.create(
                    gameId = 1L,
                    requesterUserId = 10L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 100L,
                    fieldName = "",
                    newValue = "3",
                    reason = "정정 사유",
                )
            }
        }

        @Test
        fun `newValue가 빈 문자열이면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                CorrectionRequest.create(
                    gameId = 1L,
                    requesterUserId = 10L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 100L,
                    fieldName = "hits",
                    newValue = "",
                    reason = "정정 사유",
                )
            }
        }

        @Test
        fun `reason이 빈 문자열이면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                CorrectionRequest.create(
                    gameId = 1L,
                    requesterUserId = 10L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 100L,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "",
                )
            }
        }

        @Test
        fun `reason이 공백만 있으면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                CorrectionRequest.create(
                    gameId = 1L,
                    requesterUserId = 10L,
                    correctionType = CorrectionType.BATTING,
                    targetRecordId = 100L,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "   ",
                )
            }
        }
    }

    @Nested
    @DisplayName("정정 요청 승인")
    inner class Approve {
        @Test
        fun `PENDING 상태의 요청을 승인할 수 있다`() {
            // given
            val request = createValidRequest()
            val reviewerUserId = 99L

            // when
            request.approve(reviewerUserId = reviewerUserId, comment = "승인합니다")

            // then
            assertThat(request.status).isEqualTo(CorrectionRequestStatus.APPROVED)
            assertThat(request.reviewerUserId).isEqualTo(reviewerUserId)
            assertThat(request.reviewComment).isEqualTo("승인합니다")
            assertThat(request.reviewedAt).isNotNull()
        }

        @Test
        fun `comment 없이도 승인할 수 있다`() {
            // given
            val request = createValidRequest()

            // when
            request.approve(reviewerUserId = 99L)

            // then
            assertThat(request.status).isEqualTo(CorrectionRequestStatus.APPROVED)
            assertThat(request.reviewComment).isNull()
        }

        @Test
        fun `이미 승인된 요청을 다시 승인하면 예외가 발생한다`() {
            // given
            val request = createValidRequest()
            request.approve(reviewerUserId = 99L)

            // when & then
            assertThrows<IllegalArgumentException> {
                request.approve(reviewerUserId = 99L)
            }
        }

        @Test
        fun `반려된 요청을 승인하면 예외가 발생한다`() {
            // given
            val request = createValidRequest()
            request.reject(reviewerUserId = 99L, comment = "반려 사유")

            // when & then
            assertThrows<IllegalArgumentException> {
                request.approve(reviewerUserId = 99L)
            }
        }
    }

    @Nested
    @DisplayName("정정 요청 반려")
    inner class Reject {
        @Test
        fun `PENDING 상태의 요청을 반려할 수 있다`() {
            // given
            val request = createValidRequest()
            val reviewerUserId = 99L

            // when
            request.reject(reviewerUserId = reviewerUserId, comment = "증거 불충분")

            // then
            assertThat(request.status).isEqualTo(CorrectionRequestStatus.REJECTED)
            assertThat(request.reviewerUserId).isEqualTo(reviewerUserId)
            assertThat(request.reviewComment).isEqualTo("증거 불충분")
            assertThat(request.reviewedAt).isNotNull()
        }

        @Test
        fun `반려 시 comment가 빈 문자열이면 예외가 발생한다`() {
            // given
            val request = createValidRequest()

            // when & then
            assertThrows<IllegalArgumentException> {
                request.reject(reviewerUserId = 99L, comment = "")
            }
        }

        @Test
        fun `반려 시 comment가 공백만 있으면 예외가 발생한다`() {
            // given
            val request = createValidRequest()

            // when & then
            assertThrows<IllegalArgumentException> {
                request.reject(reviewerUserId = 99L, comment = "   ")
            }
        }

        @Test
        fun `이미 승인된 요청을 반려하면 예외가 발생한다`() {
            // given
            val request = createValidRequest()
            request.approve(reviewerUserId = 99L)

            // when & then
            assertThrows<IllegalArgumentException> {
                request.reject(reviewerUserId = 99L, comment = "반려 사유")
            }
        }

        @Test
        fun `이미 반려된 요청을 다시 반려하면 예외가 발생한다`() {
            // given
            val request = createValidRequest()
            request.reject(reviewerUserId = 99L, comment = "반려 사유")

            // when & then
            assertThrows<IllegalArgumentException> {
                request.reject(reviewerUserId = 99L, comment = "재반려 사유")
            }
        }
    }
}
