package org.vladpush.exchange.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class OrderSide {
    BUY, SELL
}

data class Order(
    val id: UUID,
    val side: OrderSide,
    val ticker: String,
    val qty: Int,
    val price: BigDecimal,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)