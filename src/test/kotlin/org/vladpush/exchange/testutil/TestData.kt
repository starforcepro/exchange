package org.vladpush.exchange.testutil

import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import java.math.BigDecimal
import java.util.*

class TestOrder() {
    private var order = Order(
        id = UUID.randomUUID(),
        side = OrderSide.BUY,
        ticker = "AAPL",
        qty = 1,
        price = BigDecimal("1.00"),
    )

    fun withSide(side: OrderSide): TestOrder {
        order = order.copy(side = side)
        return this
    }

    fun withPrice(price: BigDecimal): TestOrder {
        order = order.copy(price = price)
        return this
    }

    fun withQty(qty: Int): TestOrder {
        order = order.copy(qty = qty)
        return this
    }

    fun withTicker(ticker: String): TestOrder {
        order = order.copy(ticker = ticker)
        return this
    }

    public fun build(): Order {
        return order
    }
}
