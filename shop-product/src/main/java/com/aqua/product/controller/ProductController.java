package com.aqua.product.controller;

import com.aqua.common.result.Result;
import com.aqua.product.entity.Product;
import com.aqua.product.service.ProductService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return Result.success(product);
    }

    @GetMapping("/list")
    public Result<Page<Product>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        Page<Product> result = productService.getHotProducts(page, size);
        return Result.success(result);
    }

    @GetMapping("/stock/{id}")
    public Result<Integer> getStock(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        return Result.success(product.getStock());
    }
}
