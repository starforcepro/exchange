package org.vladpush.exchange.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderQueueEntryStatus
import org.vladpush.exchange.model.OrderSide
import org.vladpush.exchange.model.OrdersQueueEntry
import org.vladpush.exchange.repository.OrderRepository
import org.vladpush.exchange.repository.OrdersQueueRepository
import org.vladpush.exchange.testutil.TestBase
import org.vladpush.exchange.testutil.TestOrder
import java.math.BigDecimal

class OrderServiceTest : TestBase() {

    @Autowired
    lateinit var service: OrderService

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var ordersQueueRepository: OrdersQueueRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanDb() {
        jdbcTemplate.execute("DELETE FROM orders_queue")
        jdbcTemplate.execute("DELETE FROM orders")
    }

    @Test
    fun `marks order checked when checked`() {
        val left = createOrder(side = OrderSide.BUY, qty = 10)

        service.handleOrders()

        val entry = ordersQueueRepository.findByOrderId(left.id)
        assertThat(entry).isNotNull
        assertThat(entry?.status).isEqualTo(OrderQueueEntryStatus.CHECKED)
    }

    @Test
    fun `aggressive order matches with 2 passive orders then stops when there is no match`() {
        val matchingPrice = BigDecimal("1.00")
        val noMatchPrice = BigDecimal("2.00")
        val firstPassiveOrder = createOrder(side = OrderSide.BUY, qty = 1, price = matchingPrice, passive = true)
        val secondPassiveOrder = createOrder(side = OrderSide.BUY, qty = 1, price = matchingPrice, passive = true)
        val noMatchPassiveOrder = createOrder(side = OrderSide.BUY, qty = 1, price = noMatchPrice, passive = true)
        val aggressiveOrder = createOrder(side = OrderSide.SELL, qty = 2, price = matchingPrice)

        service.handleOrders()

        val firstPassiveOrderUpdated = orderRepository.getById(firstPassiveOrder.id)
        val secondPassiveOrderUpdated = orderRepository.getById(secondPassiveOrder.id)
        val noMatchPassiveOrderUpdated = orderRepository.getById(noMatchPassiveOrder.id)
        val aggressiveOrderUpdated = orderRepository.getById(aggressiveOrder.id)
        assertThat(firstPassiveOrderUpdated.qty).isEqualTo(0)
        assertThat(secondPassiveOrderUpdated.qty).isEqualTo(0)
        assertThat(noMatchPassiveOrderUpdated.qty).isEqualTo(1)
        assertThat(aggressiveOrderUpdated.qty).isEqualTo(0)
    }

    @Test
    fun `does nothing when there is no aggressive order`() {
        createOrder(side = OrderSide.BUY, qty = 1, price = BigDecimal("1.00"), passive = true)
        createOrder(side = OrderSide.SELL, qty = 1, price = BigDecimal("1.00"), passive = true)

        service.handleOrders()

        val allOrders = orderRepository.findAll()
        val allQueue = ordersQueueRepository.findAll()
        assertThat(allQueue).allMatch { it.status == OrderQueueEntryStatus.CHECKED }
        assertThat(allOrders).extracting<Int> { it.qty }.containsExactlyInAnyOrder(1, 1)
    }

    @Test
    fun `aggressive BUY matches SELL by price and order, passive remains partially filled`() {
        val sellOrder = createOrder(side = OrderSide.SELL, qty = 2, price = BigDecimal("1.00"), passive = true)
        val buyOrder = createOrder(side = OrderSide.BUY, qty = 1, price = BigDecimal("2.00"))

        service.handleOrders()

        val sellUpdated = orderRepository.getById(sellOrder.id)
        val buyUpdated = orderRepository.getById(buyOrder.id)
        assertThat(buyUpdated.qty).isEqualTo(0)
        assertThat(sellUpdated.qty).isEqualTo(1)
        val buyQueue = ordersQueueRepository.getByOrderId(buyOrder.id)
        val sellQueue = ordersQueueRepository.getByOrderId(sellOrder.id)
        assertThat(buyQueue?.status).isEqualTo(OrderQueueEntryStatus.CHECKED)
        assertThat(sellQueue?.status).isEqualTo(OrderQueueEntryStatus.CHECKED)
    }

    @Test
    fun `aggressive SELL does not match BUY with lower price than required`() {
        val buyOrder = createOrder(side = OrderSide.BUY, qty = 1, price = BigDecimal("1.00"), passive = true)
        val sellOrder = createOrder(side = OrderSide.SELL, qty = 1, price = BigDecimal("2.00"))

        service.handleOrders()

        val buyUpdated = orderRepository.getById(buyOrder.id)
        val sellUpdated = orderRepository.getById(sellOrder.id)
        assertThat(sellUpdated.qty).isEqualTo(1)
        assertThat(buyUpdated.qty).isEqualTo(1)
    }

    @Test
    fun `aggressive BUY ignores different ticker`() {
        val sellOrderMsft = createOrder(side = OrderSide.SELL, ticker = "MSFT", qty = 1, price = BigDecimal("1.00"), passive = true)
        val buyOrderAapl = createOrder(side = OrderSide.BUY, ticker = "AAPL", qty = 1, price = BigDecimal("1.00"))

        service.handleOrders()

        val sellOrderMsftUpdated = orderRepository.getById(sellOrderMsft.id)
        val buyOrderAaplUpdated = orderRepository.getById(buyOrderAapl.id)
        assertThat(buyOrderAaplUpdated.qty).isEqualTo(1)
        assertThat(sellOrderMsftUpdated.qty).isEqualTo(1)
    }

    @Test
    fun `aggressive selects earliest passive when multiple eligible`() {
        val firstSellOrder = createOrder(side = OrderSide.SELL, qty = 1, price = BigDecimal("1.00"), passive = true)
        val secondSellOrder = createOrder(side = OrderSide.SELL, qty = 1, price = BigDecimal("1.00"), passive = true)
        val buyOrder = createOrder(side = OrderSide.BUY, qty = 1, price = BigDecimal("1.00"))

        service.handleOrders()

        val firstSellOrderUpdated = orderRepository.getById(firstSellOrder.id)
        val secondSellOrderUpdated = orderRepository.getById(secondSellOrder.id)
        val buyOrderUpdated = orderRepository.getById(buyOrder.id)
        assertThat(buyOrderUpdated.qty).isEqualTo(0)
        assertThat(firstSellOrderUpdated.qty).isEqualTo(0)
        assertThat(secondSellOrderUpdated.qty).isEqualTo(1)
    }

    private fun createOrder(
        side: OrderSide,
        ticker: String = "AAPL",
        qty: Int = 10,
        price: BigDecimal = BigDecimal("100.00"),
        passive: Boolean = false,
    ): Order {
        val order = TestOrder()
            .withTicker(ticker)
            .withQty(qty)
            .withSide(side)
            .withPrice(price)
            .build()
        orderRepository.save(order)
        val status = if (passive) OrderQueueEntryStatus.CHECKED else OrderQueueEntryStatus.NEW
        ordersQueueRepository.save(OrdersQueueEntry(orderId = order.id, status = status))
        return order
    }
}