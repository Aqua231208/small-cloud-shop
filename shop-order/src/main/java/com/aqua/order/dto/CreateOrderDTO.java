package com.aqua.order.dto;

import lombok.Data;

@Data
public class CreateOrderDTO {
    private Long productId;
    private Integer quantity;
}
