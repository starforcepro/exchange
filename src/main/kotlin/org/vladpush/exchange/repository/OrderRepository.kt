package org.vladpush.exchange.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

@Repository
class OrderRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    fun <T> fetchList(mapper: (ResultSet) -> T): (ResultSet) -> List<T> = { rs: ResultSet ->
        val result = mutableListOf<T>()
        while (rs.next()) {
            result.add(mapper.invoke(rs))
        }
        result
    }

    fun <T> fetchSingle(mapper: (ResultSet) -> T): (ResultSet) -> T? = { rs: ResultSet ->
        if (rs.next()) {
            mapper.invoke(rs)
        } else {
            null
        }
    }

    fun ResultSet.toOrder() = Order(
        id = UUID.fromString(this.getString("id")),
        side = OrderSide.valueOf(this.getString("side")),
        ticker = this.getString("ticker"),
        qty = this.getInt("qty"),
        price = this.getBigDecimal("price"),
        createdAt = this.getTimestamp("created_at").toLocalDateTime(),
        updatedAt = this.getTimestamp("updated_at").toLocalDateTime()
    )

    fun findAll(): List<Order> {
        val sql = "SELECT * FROM orders ORDER BY operation_number ASC"
        return jdbcTemplate.query(sql, fetchList { it.toOrder() })
    }

    fun findById(id: UUID): Order? {
        val sql = "SELECT * FROM orders WHERE id = ?"
        return jdbcTemplate.query(sql, fetchSingle { it.toOrder() }, id.toString())
    }

    fun findByTicker(ticker: String): List<Order> {
        val sql = "SELECT * FROM orders WHERE ticker = ? ORDER BY operation_number ASC"
        return jdbcTemplate.query(sql, fetchList { it.toOrder() }, ticker)
    }

    fun findTradeCandidateBy(order: Order): Order? {
        val tradeCandidateSide = when (order.side) {
            OrderSide.BUY -> OrderSide.SELL.name
            OrderSide.SELL -> OrderSide.BUY.name
        }
        val sql =
            "SELECT * FROM orders WHERE ticker = ? and price <= ? and side = ? and qty > 0 ORDER BY operation_number ASC LIMIT 1"
        return jdbcTemplate.query(sql, fetchSingle { it.toOrder() }, order.ticker, order.price, tradeCandidateSide)
    }

    @Transactional
    fun saveTrade(
        sellOrder: Order,
        buyOrder: Order,
        tradeQuantity: Int
    ): Boolean {
        val qtyDecreasedOrder1 = decreaseQty(sellOrder.id, tradeQuantity)
        val qtyDecreasedOrder2 = decreaseQty(buyOrder.id, tradeQuantity)

        if(qtyDecreasedOrder1 && qtyDecreasedOrder2) {
            return true
        } else {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            return false
        }
    }

    fun save(order: Order): Order {
        println("saving order: ${order.id}")
        val sql = """
            INSERT INTO orders (id, side, ticker, qty, price, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            updated_at = VALUES(updated_at);
        """.trimIndent()

        jdbcTemplate.update(
            sql,
            order.id.toString(),
            order.side.name,
            order.ticker,
            order.qty,
            order.price,
            order.createdAt,
            order.updatedAt
        )
        return order
    }

    private fun decreaseQty(orderId: UUID, qty: Int): Boolean {
        val sql = """
            UPDATE orders SET qty = qty - ?, updated_at = ?
            WHERE id = ? and qty >= ?
        """.trimIndent()

        return jdbcTemplate.update(sql, qty, LocalDateTime.now(), orderId.toString(), qty) > 0
    }

    fun deleteById(id: UUID) {
        val sql = "DELETE FROM orders WHERE id = ?"
        jdbcTemplate.update(sql, id.toString())
    }

    fun deleteAll() {
        val sql = "DELETE FROM orders"
        jdbcTemplate.update(sql)
    }
}