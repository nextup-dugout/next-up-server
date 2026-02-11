package com.nextup.scorer.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController {

    @GetMapping("/health")
    fun health(): ResponseEntity<HealthResponse> {
        return ResponseEntity.ok(
            HealthResponse(
                status = "UP",
                service = "next-up-scorer",
                timestamp = Instant.now().toString(),
                features = listOf("websocket", "real-time-scoring")
            )
        )
    }

    data class HealthResponse(
        val status: String,
        val service: String,
        val timestamp: String,
        val features: List<String>
    )
}
