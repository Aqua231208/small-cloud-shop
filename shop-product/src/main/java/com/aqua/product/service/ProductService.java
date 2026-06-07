package com.aqua.product.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson2.JSON;
import com.aqua.common.exception.BusinessException;
import com.aqua.product.entity.Product;
import com.aqua.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String PRODUCT_KEY_PREFIX = "p:v1:";

    @SentinelResource(value = "getProductById", blockHandler = "getProductByIdFallback")
    public Product getProductById(Long id) {
        String key = PRODUCT_KEY_PREFIX + id;
        String cachedJson = redisTemplate.opsForValue().get(key);
        if (cachedJson != null) {
            log.info("Product {} hit cache", id);
            return JSON.parseObject(cachedJson, Product.class);
        }

        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }

        redisTemplate.opsForValue().set(key, JSON.toJSONString(product), 30, TimeUnit.MINUTES);
        return product;
    }

    public Product getProductByIdFallback(Long id, BlockException e) {
        log.warn("getProductById 被限流, id={}", id);
        Product product = new Product();
        product.setId(id);
        product.setName("系统繁忙，请稍后再试");
        return product;
    }

    @SentinelResource(value = "getHotProducts", blockHandler = "getHotProductsFallback")
    public Page<Product> getHotProducts(int page, int size) {
        Page<Product> pageParam = new Page<>(page, size);
        return productMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Product>()
                        .orderByDesc(Product::getCreateTime));
    }

    public Page<Product> getHotProductsFallback(int page, int size, BlockException e) {
        log.warn("getHotProducts 被限流");
        return new Page<>(page, size);
    }
}
