package org.vladpush.exchange.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import org.vladpush.exchange.model.OrderQueueEntryStatus
import org.vladpush.exchange.model.OrdersQueueEntry
import org.vladpush.exchange.repository.OrderRepository
import org.vladpush.exchange.repository.OrdersQueueRepository
import java.math.BigDecimal
import java.util.*

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val ordersQueueRepository: OrdersQueueRepository,
) {

    @Scheduled(fixedRate = 10000L)
    fun handleOrders() {
        val aggressiveOrder = orderRepository.findAggressiveOrder() ?: return
        var passiveOrder = orderRepository.findPassiveOrderFor(aggressiveOrder)
        var aggressiveOrderQty = aggressiveOrder.qty
        while (passiveOrder != null && aggressiveOrderQty > 0) {
            val tradeQuantity = minOf(aggressiveOrderQty, passiveOrder.qty)
            val successfullyTraded = orderRepository.saveTrade(aggressiveOrder, passiveOrder, tradeQuantity)
            if (successfullyTraded) aggressiveOrderQty -= tradeQuantity
            ordersQueueRepository.updateStatus(passiveOrder.id, OrderQueueEntryStatus.CHECKED)
            passiveOrder = orderRepository.findPassiveOrderFor(aggressiveOrder)
        }
        ordersQueueRepository.updateStatus(aggressiveOrder.id, OrderQueueEntryStatus.CHECKED)
    }

    fun getAllOrders(): List<Order> {
        return orderRepository.findAll()
    }

    fun getOrderById(id: UUID): Order? {
        return orderRepository.findById(id)
    }

    @Transactional
    fun createOrder(side: OrderSide, ticker: String, qty: Int, price: BigDecimal): Order {
        validateOrderData(ticker, qty, price)

        val order = Order(
            id = UUID.randomUUID(),
            side = side,
            ticker = ticker.uppercase(),
            qty = qty,
            price = price,
        )
        orderRepository.save(order)
        ordersQueueRepository.save(OrdersQueueEntry(orderId = order.id))

        return order
    }

    fun deleteOrder(id: UUID) {
        orderRepository.deleteById(id)
    }

    fun deleteAllOrders() {
        orderRepository.deleteAll()
    }

    private fun validateOrderData(ticker: String, qty: Int, price: BigDecimal) {
        require(ticker.isNotBlank()) { "Ticker cannot be blank" }
        require(qty > 0) { "Quantity must be positive" }
        require(price > BigDecimal.ZERO) { "Price must be positive" }
    }
}