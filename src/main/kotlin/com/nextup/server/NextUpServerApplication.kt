package com.nextup.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NextUpServerApplication

fun main(args: Array<String>) {
    runApplication<NextUpServerApplication>(*args)
}
