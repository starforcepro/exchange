package org.vladpush.exchange.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import org.vladpush.exchange.service.OrderService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(OrderController::class)
class OrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var orderService: OrderService

    @Test
    fun `GET orders should return all orders`() {
        // Given
        val orders = listOf(
            createTestOrder(OrderSide.BUY, "AAPL", 100, BigDecimal("150.00")),
            createTestOrder(OrderSide.SELL, "GOOGL", 50, BigDecimal("2500.00"))
        )
        every { orderService.getAllOrders() } returns orders

        // When & Then
        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].side").value("BUY"))
            .andExpect(jsonPath("$[0].ticker").value("AAPL"))
            .andExpect(jsonPath("$[0].qty").value(100))
            .andExpect(jsonPath("$[0].price").value(150.00))
            .andExpect(jsonPath("$[1].side").value("SELL"))
            .andExpect(jsonPath("$[1].ticker").value("GOOGL"))

        verify { orderService.getAllOrders() }
    }

    @Test
    fun `GET orders by id should return order when exists`() {
        // Given
        val orderId = UUID.randomUUID()
        val order = createTestOrder(OrderSide.BUY, "AAPL", 100, BigDecimal("150.00"))
        every { orderService.getOrderById(orderId) } returns order

        // When & Then
        mockMvc.perform(get("/orders/{id}", orderId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.side").value("BUY"))
            .andExpect(jsonPath("$.ticker").value("AAPL"))
            .andExpect(jsonPath("$.qty").value(100))
            .andExpect(jsonPath("$.price").value(150.00))

        verify { orderService.getOrderById(orderId) }
    }

    @Test
    fun `GET orders by id should return 404 when not exists`() {
        // Given
        val orderId = UUID.randomUUID()
        every { orderService.getOrderById(orderId) } returns null

        // When & Then
        mockMvc.perform(get("/orders/{id}", orderId))
            .andExpect(status().isNotFound)

        verify { orderService.getOrderById(orderId) }
    }

    @Test
    fun `POST orders should create new order`() {
        // Given
        val request = CreateOrderRequest(
            side = OrderSide.BUY,
            ticker = "AAPL",
            qty = 100,
            price = BigDecimal("150.00")
        )
        val createdOrder = createTestOrder(OrderSide.BUY, "AAPL", 100, BigDecimal("150.00"))
        every { 
            orderService.createOrder(OrderSide.BUY, "AAPL", 100, BigDecimal("150.00")) 
        } returns createdOrder

        // When & Then
        mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.side").value("BUY"))
            .andExpect(jsonPath("$.ticker").value("AAPL"))
            .andExpect(jsonPath("$.qty").value(100))
            .andExpect(jsonPath("$.price").value(150.00))

        verify { orderService.createOrder(OrderSide.BUY, "AAPL", 100, BigDecimal("150.00")) }
    }

    @Test
    fun `POST orders should return 400 when validation fails`() {
        // Given
        val request = CreateOrderRequest(
            side = OrderSide.BUY,
            ticker = "",
            qty = 100,
            price = BigDecimal("150.00")
        )
        every { 
            orderService.createOrder(OrderSide.BUY, "", 100, BigDecimal("150.00")) 
        } throws IllegalArgumentException("Ticker cannot be blank")

        // When & Then
        mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)

        verify { orderService.createOrder(OrderSide.BUY, "", 100, BigDecimal("150.00")) }
    }

    @Test
    fun `DELETE orders should return 204 when order deleted`() {
        // Given
        val orderId = UUID.randomUUID()
        every { orderService.deleteOrder(orderId) }

        // When & Then
        mockMvc.perform(delete("/orders/{id}", orderId))
            .andExpect(status().isNoContent)

        verify { orderService.deleteOrder(orderId) }
    }

    @Test
    fun `DELETE orders should return 404 when order not found`() {
        // Given
        val orderId = UUID.randomUUID()
        every { orderService.deleteOrder(orderId) }

        // When & Then
        mockMvc.perform(delete("/orders/{id}", orderId))
            .andExpect(status().isNotFound)

        verify { orderService.deleteOrder(orderId) }
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