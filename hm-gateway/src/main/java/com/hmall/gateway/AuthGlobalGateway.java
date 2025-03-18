package com.hmall.gateway;

import cn.hutool.core.collection.CollUtil;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.util.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
@Component
@RequiredArgsConstructor
public class AuthGlobalGateway implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;

    private final JwtTool jwtTool;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求头中的token
        ServerHttpRequest request = exchange.getRequest();
        if (isExclude(request.getPath().toString())){
            return chain.filter(exchange);
        }
        String token = null;
        List<String> authorization = request.getHeaders().get("authorization");
        if (CollUtil.isNotEmpty(authorization)){
            token = authorization.get(0);
        }
        Long userId = null;
        try{
            userId = jwtTool.parseToken(token);
        }catch (UnauthorizedException exception){
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //jwttool校验token 传递用户信息
        String s = userId.toString();
        ServerWebExchange userInfo = exchange.mutate().request(builder -> builder.header("userInfo", s)).build();
        System.out.println("userId = "+ userId);
        // 放行无需登录url
        return chain.filter(userInfo);
    }

    private boolean isExclude(String toString) {
        List<String> excludePaths = authProperties.getExcludePaths();
        for (String excludePath : excludePaths){
            if (antPathMatcher.match(excludePath,toString)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
