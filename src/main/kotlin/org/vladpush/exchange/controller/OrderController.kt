package org.vladpush.exchange.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.vladpush.exchange.model.Order
import org.vladpush.exchange.model.OrderSide
import org.vladpush.exchange.service.OrderService
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/orders")
class OrderController(private val orderService: OrderService) {

    @GetMapping
    fun getAllOrders(): ResponseEntity<List<Order>> {
        val orders = orderService.getAllOrders()
        return ResponseEntity.ok(orders)
    }

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: UUID): ResponseEntity<Order> {
        val order = orderService.getOrderById(id)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/create")
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<Order> {
        return try {
            val order = orderService.createOrder(
                side = request.side,
                ticker = request.ticker,
                qty = request.qty,
                price = request.price
            )
            ResponseEntity.status(HttpStatus.CREATED).body(order)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/delete/{id}")
    fun deleteOrder(@PathVariable id: UUID): ResponseEntity<Void> {
        return try {
            orderService.deleteOrder(id)
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/delete/")
    fun deleteOrder(): ResponseEntity<Void> {
        return try {
            orderService.deleteAllOrders()
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
}

data class CreateOrderRequest(
    val side: OrderSide,
    val ticker: String,
    val qty: Int,
    val price: BigDecimal
)