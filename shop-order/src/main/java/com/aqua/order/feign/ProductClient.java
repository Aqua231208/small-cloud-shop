package com.aqua.order.feign;

import com.aqua.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "shop-product")
public interface ProductClient {

    @GetMapping("/product/{id}")
    Result<Map<String, Object>> getProductById(@PathVariable("id") Long id);

    @GetMapping("/product/stock/{id}")
    Result<Integer> getStock(@PathVariable("id") Long id);
}
