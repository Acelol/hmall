package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {
    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter writer;
    private final String dataId = "hm-gateway-routes.json";
    private final String groupId = "DEFAULT_GROUP";
    private final Set<String> routesIds = new HashSet<>();
    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, groupId, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }
                    @Override
                    public void receiveConfigInfo(String s) {
                        updateConfigInfo(s);
                    }
                });
    }
    private void updateConfigInfo(String configInfo) {
        log.info("更新路由信息：{}",configInfo);
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        routesIds.forEach(routeId -> {
            writer.delete(Mono.just(routeId)).subscribe();
        });
        routesIds.clear();
        routeDefinitions.forEach(routeDefinition -> {
            try {
                writer.save(Mono.just(routeDefinition)).subscribe();
                routesIds.add(routeDefinition.getId());
            } catch (Exception e) {
                log.error("更新路由信息失败：{}",e.getMessage());
            }
        });
    }
}
