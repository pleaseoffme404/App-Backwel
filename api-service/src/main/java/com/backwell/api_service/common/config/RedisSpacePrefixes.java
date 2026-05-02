package com.backwell.api_service.common.config;


import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@Data
@ConfigurationProperties(prefix = "app.redis.namespaces")
public class RedisSpacePrefixes {
    private String stock;
    private String referralCodes;
}
