package com.shortlink.service.impl;

import com.shortlink.mapper.ShortLinkMapper;
import com.shortlink.service.VisitCountService;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
public class VisitCountServiceImpl implements VisitCountService {
    private static final String COUNT_CACHE_PREFIX = "link:count:";

    private final ShortLinkMapper shortLinkMapper;
    private final StringRedisTemplate redisTemplate;

    public VisitCountServiceImpl(ShortLinkMapper shortLinkMapper, StringRedisTemplate redisTemplate) {
        this.shortLinkMapper = shortLinkMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Async("visitCountExecutor")
    public void incrementVisitAsync(String shortCode) {
        redisTemplate.opsForValue().increment(COUNT_CACHE_PREFIX + shortCode);
    }

    @Override
    public void deleteVisitCount(String shortCode) {
        redisTemplate.delete(COUNT_CACHE_PREFIX + shortCode);
    }

    @Override
    public void flushVisitCounts() {
        ScanOptions options = ScanOptions.scanOptions()
                .match(COUNT_CACHE_PREFIX + "*")
                .count(100)
                .build();
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
             Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                flushVisitCountKey(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        }
    }

    private void flushVisitCountKey(String key) {
        String countValue = redisTemplate.opsForValue().get(key);
        long delta = parseLong(countValue);
        if (delta <= 0) {
            redisTemplate.delete(key);
            return;
        }
        String shortCode = key.substring(COUNT_CACHE_PREFIX.length());
        shortLinkMapper.incrementVisitCount(shortCode, delta);
        redisTemplate.delete(key);
    }

    private long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
