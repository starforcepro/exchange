package org.vladpush.exchange.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.vladpush.exchange.model.OrderQueueEntryStatus
import org.vladpush.exchange.model.OrdersQueueEntry
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

@Repository
class OrdersQueueRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    private fun <T> fetchList(mapper: (ResultSet) -> T): (ResultSet) -> List<T> = { rs: ResultSet ->
        val result = mutableListOf<T>()
        while (rs.next()) {
            result.add(mapper.invoke(rs))
        }
        result
    }

    private fun <T> fetchSingle(mapper: (ResultSet) -> T): (ResultSet) -> T? = { rs: ResultSet ->
        if (rs.next()) mapper.invoke(rs) else null
    }

    private fun ResultSet.toOrdersQueue() = OrdersQueueEntry(
        id = UUID.fromString(this.getString("id")),
        orderId = UUID.fromString(this.getString("order_id")),
        status = OrderQueueEntryStatus.valueOf(this.getString("status")),
        createdAt = this.getTimestamp("created_at").toLocalDateTime(),
        updatedAt = this.getTimestamp("updated_at").toLocalDateTime()
    )

    fun findAll(): List<OrdersQueueEntry> {
        val sql = "SELECT * FROM orders_queue ORDER BY position ASC"
        return jdbcTemplate.query(sql, fetchList { it.toOrdersQueue() })
    }

    fun findById(id: UUID): OrdersQueueEntry? {
        val sql = "SELECT * FROM orders_queue WHERE id = ?"
        return jdbcTemplate.query(sql, fetchSingle { it.toOrdersQueue() }, id)
    }

    fun findByOrderId(id: UUID): OrdersQueueEntry? {
        val sql = "SELECT * FROM orders_queue WHERE order_id = ?"
        return jdbcTemplate.query(sql, fetchSingle { it.toOrdersQueue() }, id)
    }

    fun getByOrderId(id: UUID): OrdersQueueEntry? {
        val sql = "SELECT * FROM orders_queue WHERE order_id = ?"
        return jdbcTemplate.query(sql, fetchSingle { it.toOrdersQueue() }, id)
    }


    fun save(entry: OrdersQueueEntry): OrdersQueueEntry {
        val sql = """
            INSERT INTO orders_queue (id, order_id, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        jdbcTemplate.update(
            sql,
            entry.id,
            entry.orderId,
            entry.status.name,
            entry.createdAt,
            entry.updatedAt
        )
        return entry
    }

    fun updateStatus(orderId: UUID, status: OrderQueueEntryStatus): Boolean {
        val sql = """
            UPDATE orders_queue SET status = ?, updated_at = ?
            WHERE order_id = ?
        """.trimIndent()
        return jdbcTemplate.update(sql, status.name, LocalDateTime.now(), orderId) > 0
    }
}
