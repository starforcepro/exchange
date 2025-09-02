package org.vladpush.exchange

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ExchangeApplication

fun main(args: Array<String>) {
    runApplication<ExchangeApplication>(*args)
}