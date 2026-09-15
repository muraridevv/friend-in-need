package com.friendinneed.routing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RoutingConfiguration {
    @Bean HealthPinger openRouterHealthPinger() { return new HealthPinger(); }
    @Bean HealthPinger voiceHealthPinger() { return new HealthPinger(); }
    @Bean HealthPinger embeddingHealthPinger() { return new HealthPinger(); }
}
