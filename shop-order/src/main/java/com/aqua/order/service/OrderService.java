package com.aqua.order.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.aqua.common.context.UserContext;
import com.aqua.common.exception.BusinessException;
import com.aqua.common.result.Result;
import com.aqua.order.dto.CreateOrderDTO;
import com.aqua.order.entity.Order;
import com.aqua.order.feign.ProductClient;
import com.aqua.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final ProductClient productClient;

    @SentinelResource(value = "createOrder", blockHandler = "createOrderFallback")
    public Order createOrder(CreateOrderDTO dto) {
        Long userId = UserContext.getUserId();

        // Call product service via Feign
        Result<Map<String, Object>> productResult = productClient.getProductById(dto.getProductId());
        if (productResult.getCode() != 200 || productResult.getData() == null) {
            throw new BusinessException("商品不存在");
        }

        Map<String, Object> productData = productResult.getData();
        String productName = (String) productData.get("name");
        BigDecimal price = new BigDecimal(productData.get("price").toString());
        Integer stock = (Integer) productData.get("stock");

        if (stock < dto.getQuantity()) {
            throw new BusinessException("库存不足");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setProductId(dto.getProductId());
        order.setProductName(productName);
        order.setPrice(price);
        order.setQuantity(dto.getQuantity());
        order.setTotalAmount(price.multiply(BigDecimal.valueOf(dto.getQuantity())));
        order.setStatus("PENDING");
        orderMapper.insert(order);

        log.info("Order created: id={}, userId={}, amount={}", order.getId(), userId, order.getTotalAmount());
        return order;
    }

    public Order createOrderFallback(CreateOrderDTO dto, BlockException e) {
        log.warn("createOrder 被限流, productId={}", dto.getProductId());
        throw new BusinessException(429, "下单过于频繁，请稍后再试");
    }

    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }
}
