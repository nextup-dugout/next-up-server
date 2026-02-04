package com.nextup.backoffice.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController {
    @GetMapping("/health")
    fun health(): ResponseEntity<HealthResponse> =
        ResponseEntity.ok(
            HealthResponse(
                status = "UP",
                service = "next-up-backoffice",
                timestamp = Instant.now().toString(),
            ),
        )

    data class HealthResponse(
        val status: String,
        val service: String,
        val timestamp: String,
    )
}
