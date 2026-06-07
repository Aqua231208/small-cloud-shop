package com.aqua.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import jakarta.annotation.PostConstruct;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import java.util.Map;

@Configuration
public class SentinelGatewayConfig {

    @PostConstruct
    public void init() {
        BlockRequestHandler blockHandler = (exchange, t) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromValue(Map.of(
                                "code", 429,
                                "message", "请求过于频繁，请稍后再试",
                                "data", null
                        )));
        GatewayCallbackManager.setBlockHandler(blockHandler);
    }
}
