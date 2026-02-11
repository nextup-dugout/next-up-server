package com.nextup.api.dto.game

import com.nextup.core.domain.game.PlateAppearanceResult
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordPlateAppearanceRequestTest {
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `should create request with valid data`() {
        // when
        val request =
            RecordPlateAppearanceRequest(
                result = PlateAppearanceResult.SINGLE,
                runsBattedIn = 1,
                runsScored = true,
            )

        // then
        assertThat(request.result).isEqualTo(PlateAppearanceResult.SINGLE)
        assertThat(request.runsBattedIn).isEqualTo(1)
        assertThat(request.runsScored).isTrue()
    }

    @Test
    fun `should use default values`() {
        // when
        val request =
            RecordPlateAppearanceRequest(
                result = PlateAppearanceResult.HOME_RUN,
            )

        // then
        assertThat(request.runsBattedIn).isEqualTo(0)
        assertThat(request.runsScored).isFalse()
    }

    @Test
    fun `should pass validation with valid data`() {
        // given
        val request =
            RecordPlateAppearanceRequest(
                result = PlateAppearanceResult.DOUBLE,
                runsBattedIn = 2,
                runsScored = false,
            )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    @Test
    fun `should fail validation when runsBattedIn is negative`() {
        // given
        val request =
            RecordPlateAppearanceRequest(
                result = PlateAppearanceResult.SINGLE,
                runsBattedIn = -1,
                runsScored = false,
            )

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isNotEmpty
        assertThat(violations.first().message).contains("0 이상")
    }

    @Test
    fun `should support data class copy`() {
        // given
        val original =
            RecordPlateAppearanceRequest(
                result = PlateAppearanceResult.SINGLE,
                runsBattedIn = 1,
                runsScored = true,
            )

        // when
        val copied = original.copy(runsBattedIn = 2)

        // then
        assertThat(copied.runsBattedIn).isEqualTo(2)
        assertThat(original.runsBattedIn).isEqualTo(1)
    }
}
