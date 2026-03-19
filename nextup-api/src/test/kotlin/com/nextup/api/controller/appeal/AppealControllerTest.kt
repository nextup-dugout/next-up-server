package com.nextup.api.controller.appeal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nextup.api.dto.appeal.CreateAppealApiRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.appeal.Appeal
import com.nextup.core.domain.appeal.AppealStatus
import com.nextup.core.domain.appeal.AppealType
import com.nextup.core.domain.game.Game
import com.nextup.core.service.appeal.AppealService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

@DisplayName("AppealController")
class AppealControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var appealService: AppealService
    private lateinit var objectMapper: ObjectMapper

    private val mockGame = mockk<Game>(relaxed = true)
    private val mockAppeal = mockk<Appeal>(relaxed = true)

    @BeforeEach
    fun setUp() {
        appealService = mockk()
        objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

        val controller = AppealController(appealService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                    AuthenticationPrincipalResolver(100L),
                )
                .build()

        every { mockGame.id } returns 1L
        every { mockAppeal.id } returns 1L
        every { mockAppeal.game } returns mockGame
        every { mockAppeal.appealerId } returns 100L
        every { mockAppeal.appealerName } returns "홍길동"
        every { mockAppeal.type } returns AppealType.SCORING_ERROR
        every { mockAppeal.title } returns "득점 오류 정정 요청"
        every { mockAppeal.description } returns "3회말 득점이 잘못 기록되었습니다"
        every { mockAppeal.status } returns AppealStatus.PENDING
        every { mockAppeal.reviewerId } returns null
        every { mockAppeal.reviewerComment } returns null
        every { mockAppeal.reviewedAt } returns null
        every { mockAppeal.createdAt } returns Instant.now()
    }

    @Test
    fun `should create appeal successfully`() {
        // given
        val request =
            CreateAppealApiRequest(
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "득점 오류 정정 요청",
                description = "3회말 득점이 잘못 기록되었습니다",
            )

        every { appealService.createAppeal(any()) } returns mockAppeal

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/1/appeals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.gameId").value(1))
            .andExpect(jsonPath("$.data.appealerId").value(100))
            .andExpect(jsonPath("$.data.appealerName").value("홍길동"))
            .andExpect(jsonPath("$.data.type").value("SCORING_ERROR"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))

        verify(exactly = 1) { appealService.createAppeal(any()) }
    }

    @Test
    fun `should fail to create appeal with invalid request`() {
        // given
        val invalidRequest =
            mapOf(
                "appealerId" to -1,
                "appealerName" to "",
                "type" to "SCORING_ERROR",
                "title" to "",
                "description" to "",
            )

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/1/appeals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))

        verify(exactly = 0) { appealService.createAppeal(any()) }
    }

    @Test
    fun `should fail to create appeal when game not found`() {
        // given
        val request =
            CreateAppealApiRequest(
                appealerId = 100L,
                appealerName = "홍길동",
                type = AppealType.SCORING_ERROR,
                title = "제목",
                description = "설명",
            )

        every { appealService.createAppeal(any()) } throws GameNotFoundException(999L)

        // when & then
        mockMvc
            .perform(
                post("/api/v1/games/999/appeals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `should get my appeals`() {
        // given
        every { appealService.getAppealsByAppealer(100L) } returns listOf(mockAppeal)

        // when & then
        mockMvc
            .perform(get("/api/v1/appeals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].appealerId").value(100))

        verify(exactly = 1) { appealService.getAppealsByAppealer(100L) }
    }

    @Test
    fun `should get empty list when no appeals found for appealer`() {
        // given
        every { appealService.getAppealsByAppealer(100L) } returns emptyList()

        // when & then
        mockMvc
            .perform(get("/api/v1/appeals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)

        verify(exactly = 1) { appealService.getAppealsByAppealer(100L) }
    }

    @Test
    fun `should get game appeals`() {
        // given
        every { appealService.getAppealsByGame(1L) } returns listOf(mockAppeal)

        // when & then
        mockMvc
            .perform(get("/api/v1/games/1/appeals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].gameId").value(1))

        verify(exactly = 1) { appealService.getAppealsByGame(1L) }
    }

    @Test
    fun `should get empty list when no appeals found for game`() {
        // given
        every { appealService.getAppealsByGame(999L) } returns emptyList()

        // when & then
        mockMvc
            .perform(get("/api/v1/games/999/appeals"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data").isEmpty)

        verify(exactly = 1) { appealService.getAppealsByGame(999L) }
    }

    @Test
    fun `should handle different appeal types`() {
        // given
        val types =
            listOf(
                AppealType.SCORING_ERROR,
                AppealType.RECORD_CORRECTION,
                AppealType.RULE_VIOLATION,
                AppealType.OTHER,
            )

        types.forEach { type ->
            val request =
                CreateAppealApiRequest(
                    appealerId = 100L,
                    appealerName = "홍길동",
                    type = type,
                    title = "제목",
                    description = "설명",
                )

            val mockAppealWithType = mockk<Appeal>(relaxed = true)
            every { mockAppealWithType.id } returns 1L
            every { mockAppealWithType.game } returns mockGame
            every { mockAppealWithType.appealerId } returns 100L
            every { mockAppealWithType.appealerName } returns "홍길동"
            every { mockAppealWithType.type } returns type
            every { mockAppealWithType.title } returns "제목"
            every { mockAppealWithType.description } returns "설명"
            every { mockAppealWithType.status } returns AppealStatus.PENDING
            every { mockAppealWithType.reviewerId } returns null
            every { mockAppealWithType.reviewerComment } returns null
            every { mockAppealWithType.reviewedAt } returns null
            every { mockAppealWithType.createdAt } returns Instant.now()

            every { appealService.createAppeal(any()) } returns mockAppealWithType

            // when & then
            mockMvc
                .perform(
                    post("/api/v1/games/1/appeals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value(type.name))
        }
    }
}

/**
 * 테스트용 @AuthenticationPrincipal 리졸버
 */
private class AuthenticationPrincipalResolver(
    private val userId: Long,
) : org.springframework.web.method.support.HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: org.springframework.core.MethodParameter): Boolean =
        parameter.hasParameterAnnotation(
            org.springframework.security.core.annotation.AuthenticationPrincipal::class.java,
        )

    override fun resolveArgument(
        parameter: org.springframework.core.MethodParameter,
        mavContainer: org.springframework.web.method.support.ModelAndViewContainer?,
        webRequest: org.springframework.web.context.request.NativeWebRequest,
        binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
    ): Any = userId
}
