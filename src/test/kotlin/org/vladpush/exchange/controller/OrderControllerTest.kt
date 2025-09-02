package org.vladpush.exchange.controller

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import org.vladpush.exchange.repository.OrderRepository
import org.vladpush.exchange.testutil.TestBase
import java.math.BigDecimal
import java.time.Duration
import java.util.*


class OrderControllerTest : TestBase() {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    private fun createOrder(): Order {
        val order = Order(
            id = UUID.randomUUID(),
            side = OrderSide.BUY,
            ticker = "AAPL",
            qty = 1,
            price = BigDecimal("1.00"),
        )
        orderRepository.save(order)
        return order
    }

    @Test
    fun savesOrder() {
        val create = CreateOrderRequest(OrderSide.BUY, "AAPL", 10, BigDecimal("1"))

        val createResponse = restTemplate.postForEntity(baseUrl() + "/orders/create", create, Order::class.java)

        val created = createResponse.body!!
        await.atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(100))
            .until { orderRepository.findById(created.id) != null }
        Assertions.assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
    }

    @Test
    fun deletesOrderById() {
        val storedOrder = createOrder()

        val deleteResponse = restTemplate.postForEntity(
            baseUrl() + "/orders/delete/${storedOrder.id}",
            HttpEntity.EMPTY,
            Void::class.java
        )

        val storedOrderAfterDeletion = orderRepository.findById(storedOrder.id)
        Assertions.assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(storedOrderAfterDeletion).isNull()
    }

    @Test
    fun getsOrderById() {
        val storedOrder = createOrder()

        val getResponse = restTemplate.getForEntity(baseUrl() + "/orders/${storedOrder.id}", Order::class.java)
        val order = getResponse.body!!

        Assertions.assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(storedOrder.id).isEqualTo(order.id)
    }
}
