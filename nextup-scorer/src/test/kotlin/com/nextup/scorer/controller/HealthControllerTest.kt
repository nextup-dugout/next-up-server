package com.nextup.scorer.controller

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("HealthController (Scorer)")
class HealthControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var controller: HealthController

    @BeforeEach
    fun setUp() {
        controller = HealthController()
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `should return health status`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("next-up-scorer"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.features").isArray)
            .andExpect(jsonPath("$.features.length()").value(2))
    }
}
