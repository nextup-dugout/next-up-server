package com.nextup.api.dto.game

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordPitchingEventRequestTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `should create RecordOut request`() {
        // when
        val request = RecordPitchingEventRequest.RecordOut(isStrikeout = true)

        // then
        assertThat(request).isInstanceOf(RecordPitchingEventRequest::class.java)
        assertThat(request.isStrikeout).isTrue()
    }

    @Test
    fun `should create RecordHit request with valid data`() {
        // when
        val request =
            RecordPitchingEventRequest.RecordHit(
                isHomeRun = true,
                runsScored = 1,
                earnedRuns = 1,
            )

        // then
        assertThat(request.isHomeRun).isTrue()
        assertThat(request.runsScored).isEqualTo(1)
        assertThat(request.earnedRuns).isEqualTo(1)
    }

    @Test
    fun `should fail validation when runsScored is negative`() {
        // given
        val request =
            RecordPitchingEventRequest.RecordHit(
                isHomeRun = false,
                runsScored = -1,
                earnedRuns = 0,
            )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `should create RecordWalk request`() {
        // when
        val request = RecordPitchingEventRequest.RecordWalk()

        // then
        assertThat(request).isInstanceOf(RecordPitchingEventRequest::class.java)
    }

    @Test
    fun `should create RecordHitByPitch request`() {
        // when
        val request = RecordPitchingEventRequest.RecordHitByPitch()

        // then
        assertThat(request).isInstanceOf(RecordPitchingEventRequest::class.java)
    }

    @Test
    fun `should create RecordPitchCount request with valid data`() {
        // when
        val request =
            RecordPitchingEventRequest.RecordPitchCount(
                totalPitches = 100,
                strikes = 65,
            )

        // then
        assertThat(request.totalPitches).isEqualTo(100)
        assertThat(request.strikes).isEqualTo(65)
    }

    @Test
    fun `should fail validation when totalPitches is less than 1`() {
        // given
        val request =
            RecordPitchingEventRequest.RecordPitchCount(
                totalPitches = 0,
                strikes = 0,
            )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
    }

    @Test
    fun `should pass validation with valid RecordPitchCount`() {
        // given
        val request =
            RecordPitchingEventRequest.RecordPitchCount(
                totalPitches = 1,
                strikes = 1,
            )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }
}
