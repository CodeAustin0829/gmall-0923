package com.atguigu.gmallgate.way.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/26
 */
//@Component // 需要将拦截器放入到Spring容器中，才能进行拦截
public class TestGatewayFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("无需配置，拦截所有经过网关的请求！！");
        // 放行
        return chain.filter(exchange);
    }
}
