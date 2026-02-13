package com.nextup.scorer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.nextup"])
class ScorerApplication

fun main(args: Array<String>) {
    runApplication<ScorerApplication>(*args)
}
