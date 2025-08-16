package org.vladpush.exchange.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import org.vladpush.exchange.repository.OrderRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class OrderServiceTest {

    private val orderRepository: OrderRepository = mockk()
    private val orderService = OrderService(orderRepository)

    @Test
    fun `getAllOrders should return all orders from repository`() {
        // Given
        val orders = listOf(
            createTestOrder(OrderSide.BUY, "AAPL", 100, BigDecimal("150.00")),
            createTestOrder(OrderSide.SELL, "GOOGL", 50, BigDecimal("2500.00"))
        )
        every { orderRepository.findAll() } returns orders

        // When
        val result = orderService.getAllOrders()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsOf(orders)
        verify { orderRepository.findAll() }
    }

    @Test
    fun `createOrder should save order with generated UUID and timestamps`() {
        // Given
        val orderSlot = slot<Order>()
        every { orderRepository.save(capture(orderSlot)) } answers { orderSlot.captured }

        // When
        val result = orderService.createOrder(
            side = OrderSide.BUY,
            ticker = "aapl",
            qty = 100,
            price = BigDecimal("150.00")
        )

        // Then
        assertThat(result.id).isNotNull()
        assertThat(result.side).isEqualTo(OrderSide.BUY)
        assertThat(result.ticker).isEqualTo("AAPL") // Should be uppercased
        assertThat(result.qty).isEqualTo(100)
        assertThat(result.price).isEqualTo(BigDecimal("150.00"))
        assertThat(result.createdAt).isNotNull()
        assertThat(result.updatedAt).isNotNull()
        assertThat(result.createdAt).isEqualTo(result.updatedAt)
        verify { orderRepository.save(any()) }
    }

    @Test
    fun `createOrder should validate ticker is not blank`() {
        // When & Then
        assertThatThrownBy {
            orderService.createOrder(
                side = OrderSide.BUY,
                ticker = "",
                qty = 100,
                price = BigDecimal("150.00")
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Ticker cannot be blank")
    }

    @Test
    fun `createOrder should validate quantity is positive`() {
        // When & Then
        assertThatThrownBy {
            orderService.createOrder(
                side = OrderSide.BUY,
                ticker = "AAPL",
                qty = 0,
                price = BigDecimal("150.00")
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Quantity must be positive")
    }

    @Test
    fun `createOrder should validate price is positive`() {
        // When & Then
        assertThatThrownBy {
            orderService.createOrder(
                side = OrderSide.BUY,
                ticker = "AAPL",
                qty = 100,
                price = BigDecimal.ZERO
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Price must be positive")
    }

    @Test
    fun `getOrderById should return order when exists`() {
        // Given
        val orderId = UUID.randomUUID()
        val order = createTestOrder(OrderSide.BUY, "AAPL", 100, BigDecimal("150.00"))
        every { orderRepository.findById(orderId) } returns order

        // When
        val result = orderService.getOrderById(orderId)

        // Then
        assertThat(result).isEqualTo(order)
        verify { orderRepository.findById(orderId) }
    }

    @Test
    fun `getOrderById should return null when not exists`() {
        // Given
        val orderId = UUID.randomUUID()
        every { orderRepository.findById(orderId) } returns null

        // When
        val result = orderService.getOrderById(orderId)

        // Then
        assertThat(result).isNull()
        verify { orderRepository.findById(orderId) }
    }

    private fun createTestOrder(
        side: OrderSide,
        ticker: String,
        qty: Int,
        price: BigDecimal
    ): Order {
        val now = LocalDateTime.now()
        return Order(
            id = UUID.randomUUID(),
            side = side,
            ticker = ticker,
            qty = qty,
            price = price,
            createdAt = now,
            updatedAt = now
        )
    }
}