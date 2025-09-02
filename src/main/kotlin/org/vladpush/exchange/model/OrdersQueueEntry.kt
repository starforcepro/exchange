package org.vladpush.exchange.model

import java.time.LocalDateTime
import java.util.UUID

data class OrdersQueueEntry(
    val id: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val status: OrderQueueEntryStatus = OrderQueueEntryStatus.NEW,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class OrderQueueEntryStatus {
    NEW, CHECKED
}
