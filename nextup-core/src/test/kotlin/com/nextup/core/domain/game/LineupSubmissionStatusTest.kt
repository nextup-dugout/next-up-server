package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LineupSubmissionStatusTest {
    @Test
    fun `canSubmit returns true only for DRAFT, REJECTED, EXCHANGE_REJECTED`() {
        assertThat(LineupSubmissionStatus.DRAFT.canSubmit()).isTrue()
        assertThat(LineupSubmissionStatus.REJECTED.canSubmit()).isTrue()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.canSubmit()).isTrue()

        assertThat(LineupSubmissionStatus.SUBMITTED.canSubmit()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.canSubmit()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.canSubmit()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGED.canSubmit()).isFalse()
    }

    @Test
    fun `canEdit returns true only for DRAFT, REJECTED, EXCHANGE_REJECTED`() {
        assertThat(LineupSubmissionStatus.DRAFT.canEdit()).isTrue()
        assertThat(LineupSubmissionStatus.REJECTED.canEdit()).isTrue()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.canEdit()).isTrue()

        assertThat(LineupSubmissionStatus.SUBMITTED.canEdit()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.canEdit()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.canEdit()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGED.canEdit()).isFalse()
    }

    @Test
    fun `canConfirm returns true only for SUBMITTED`() {
        assertThat(LineupSubmissionStatus.SUBMITTED.canConfirm()).isTrue()

        assertThat(LineupSubmissionStatus.DRAFT.canConfirm()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.canConfirm()).isFalse()
        assertThat(LineupSubmissionStatus.REJECTED.canConfirm()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.canConfirm()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.canConfirm()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGED.canConfirm()).isFalse()
    }

    @Test
    fun `canReject returns true only for SUBMITTED`() {
        assertThat(LineupSubmissionStatus.SUBMITTED.canReject()).isTrue()

        assertThat(LineupSubmissionStatus.DRAFT.canReject()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.canReject()).isFalse()
        assertThat(LineupSubmissionStatus.REJECTED.canReject()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.canReject()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.canReject()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGED.canReject()).isFalse()
    }

    @Test
    fun `canMarkExchangePending returns true only for SUBMITTED`() {
        assertThat(LineupSubmissionStatus.SUBMITTED.canMarkExchangePending()).isTrue()

        assertThat(LineupSubmissionStatus.DRAFT.canMarkExchangePending()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.canMarkExchangePending()).isFalse()
        assertThat(LineupSubmissionStatus.REJECTED.canMarkExchangePending()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.canMarkExchangePending()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.canMarkExchangePending()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGED.canMarkExchangePending()).isFalse()
    }

    @Test
    fun `canApproveExchange returns true only for EXCHANGE_PENDING`() {
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.canApproveExchange()).isTrue()

        assertThat(LineupSubmissionStatus.DRAFT.canApproveExchange()).isFalse()
        assertThat(LineupSubmissionStatus.SUBMITTED.canApproveExchange()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.canApproveExchange()).isFalse()
        assertThat(LineupSubmissionStatus.REJECTED.canApproveExchange()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.canApproveExchange()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGED.canApproveExchange()).isFalse()
    }

    @Test
    fun `canRejectExchange returns true only for EXCHANGE_PENDING`() {
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.canRejectExchange()).isTrue()

        assertThat(LineupSubmissionStatus.DRAFT.canRejectExchange()).isFalse()
        assertThat(LineupSubmissionStatus.SUBMITTED.canRejectExchange()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.canRejectExchange()).isFalse()
        assertThat(LineupSubmissionStatus.REJECTED.canRejectExchange()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.canRejectExchange()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGED.canRejectExchange()).isFalse()
    }

    @Test
    fun `canRevertToSubmitted returns true only for EXCHANGE_PENDING`() {
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.canRevertToSubmitted()).isTrue()

        assertThat(LineupSubmissionStatus.DRAFT.canRevertToSubmitted()).isFalse()
        assertThat(LineupSubmissionStatus.SUBMITTED.canRevertToSubmitted()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.canRevertToSubmitted()).isFalse()
        assertThat(LineupSubmissionStatus.REJECTED.canRevertToSubmitted()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.canRevertToSubmitted()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGED.canRevertToSubmitted()).isFalse()
    }

    @Test
    fun `isVisibleToOpponent returns true only for EXCHANGED`() {
        assertThat(LineupSubmissionStatus.EXCHANGED.isVisibleToOpponent()).isTrue()

        assertThat(LineupSubmissionStatus.DRAFT.isVisibleToOpponent()).isFalse()
        assertThat(LineupSubmissionStatus.SUBMITTED.isVisibleToOpponent()).isFalse()
        assertThat(LineupSubmissionStatus.CONFIRMED.isVisibleToOpponent()).isFalse()
        assertThat(LineupSubmissionStatus.REJECTED.isVisibleToOpponent()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_PENDING.isVisibleToOpponent()).isFalse()
        assertThat(LineupSubmissionStatus.EXCHANGE_REJECTED.isVisibleToOpponent()).isFalse()
    }

    @Test
    fun `all statuses have non-blank displayName`() {
        LineupSubmissionStatus.entries.forEach { status ->
            assertThat(status.displayName).isNotBlank()
        }
    }
}
