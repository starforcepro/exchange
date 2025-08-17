package org.vladpush.exchange.service

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.vladpush.exchange.config.KafkaConfig
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import org.vladpush.exchange.repository.OrderRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val kafkaTemplate: KafkaTemplate<String, Order>,
    private val kafkaConfig: KafkaConfig,
) {
    
//    @Value("\${exchange.kafka.topic.orders}")
//    private lateinit var ordersTopic: String

    fun getAllOrders(): List<Order> {
        return orderRepository.findAll()
    }

    fun getOrderById(id: UUID): Order? {
        return orderRepository.findById(id)
    }

    fun createOrder(side: OrderSide, ticker: String, qty: Int, price: BigDecimal): Order {
        validateOrderData(ticker, qty, price)
        
        val now = LocalDateTime.now()
        val order = Order(
            id = UUID.randomUUID(),
            side = side,
            ticker = ticker.uppercase(),
            qty = qty,
            price = price,
            createdAt = now,
            updatedAt = now
        )

        kafkaTemplate.send(kafkaConfig.ordersTopicName, order.id.toString(), order)
        
        return order
    }

    fun deleteOrder(id: UUID) {
        orderRepository.deleteById(id)
    }

    private fun validateOrderData(ticker: String, qty: Int, price: BigDecimal) {
        require(ticker.isNotBlank()) { "Ticker cannot be blank" }
        require(qty > 0) { "Quantity must be positive" }
        require(price > BigDecimal.ZERO) { "Price must be positive" }
    }
}