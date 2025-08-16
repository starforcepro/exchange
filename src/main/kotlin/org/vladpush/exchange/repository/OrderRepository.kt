package org.vladpush.exchange.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import java.sql.ResultSet
import java.util.*

@Repository
class OrderRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    private val orderRowMapper = RowMapper<Order> { rs: ResultSet, _: Int ->
        Order(
            id = UUID.fromString(rs.getString("id")),
            side = OrderSide.valueOf(rs.getString("side")),
            ticker = rs.getString("ticker"),
            qty = rs.getInt("qty"),
            price = rs.getBigDecimal("price"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = rs.getTimestamp("updated_at").toLocalDateTime()
        )
    }

    fun findAll(): List<Order> {
        val sql = "SELECT * FROM orders ORDER BY operation_number ASC"
        return jdbcTemplate.query(sql, orderRowMapper)
    }

    fun findById(id: UUID): Order? {
        val sql = "SELECT * FROM orders WHERE id = ?"
        return try {
            jdbcTemplate.queryForObject(sql, orderRowMapper, id.toString())
        } catch (e: Exception) {
            null
        }
    }

    fun findByTicker(ticker: String): Order? {
        val sql = "SELECT * FROM orders WHERE ticker = ? ORDER BY operation_number ASC"
        return try {
            jdbcTemplate.queryForObject(sql, orderRowMapper, ticker)
        } catch (e: Exception) {
            null
        }
    }

    fun save(order: Order): Order {
        val sql = """
            INSERT INTO orders (id, side, ticker, qty, price, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
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

    fun deleteById(id: UUID) {
        val sql = "DELETE FROM orders WHERE id = ?"
        jdbcTemplate.update(sql, id.toString())
    }
}