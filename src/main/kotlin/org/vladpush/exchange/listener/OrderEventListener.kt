package org.vladpush.exchange.listener

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.repository.OrderRepository

@Component
class OrderEventListener(
    private val orderRepository: OrderRepository,
) {

    @KafkaListener(
        topics = ["\${exchange.kafka.topic.orders}"],
        groupId = "\${spring.kafka.consumer.group-id}",
        properties = ["spring.json.value.default.type=org.vladpush.exchange.model.Order"]
    )
    fun handleOrderEvent(newOrder: Order) {
        orderRepository.save(newOrder)
        var tradeCandidate = orderRepository.findTradeCandidateBy(newOrder)
        var newOrderQty = newOrder.qty
        while (tradeCandidate != null && newOrderQty > 0) {
            val tradeQuantity = minOf(newOrderQty, tradeCandidate.qty)
            val successfullyTraded = orderRepository.saveTrade(newOrder, tradeCandidate, tradeQuantity)
            if (successfullyTraded) newOrderQty -= tradeQuantity
            tradeCandidate = orderRepository.findTradeCandidateBy(newOrder)
        }
    }
}