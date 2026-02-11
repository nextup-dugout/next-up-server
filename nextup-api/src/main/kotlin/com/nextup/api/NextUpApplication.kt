package com.nextup.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.nextup"])
class NextUpApplication

fun main(args: Array<String>) {
    runApplication<NextUpApplication>(*args)
}
