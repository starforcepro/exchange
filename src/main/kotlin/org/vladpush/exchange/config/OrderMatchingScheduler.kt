package org.vladpush.exchange.config

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.vladpush.exchange.service.OrderService

@Profile("!test")
@Component
class OrderMatchingScheduler(private val orderService: OrderService) {

    @Scheduled(fixedRate = 10_000L)
    fun handleOrders() = orderService.handleOrders()
}