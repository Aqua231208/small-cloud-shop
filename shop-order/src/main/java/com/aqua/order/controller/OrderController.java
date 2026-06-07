package com.aqua.order.controller;

import com.aqua.common.context.UserContext;
import com.aqua.common.result.Result;
import com.aqua.order.dto.CreateOrderDTO;
import com.aqua.order.entity.Order;
import com.aqua.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public Result<Order> create(@RequestBody CreateOrderDTO dto,
                                 @RequestHeader("X-User-Id") Long userId) {
        UserContext.setUserId(userId);
        try {
            Order order = orderService.createOrder(dto);
            return Result.success(order);
        } finally {
            UserContext.clear();
        }
    }

    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return Result.success(order);
    }
}
