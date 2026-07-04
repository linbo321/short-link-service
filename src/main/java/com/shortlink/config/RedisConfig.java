package com.shortlink.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
    private final StringRedisTemplate redisTemplate;

    public RedisConfig(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public StringRedisTemplate redisTemplate() {
        return redisTemplate;
    }
}
