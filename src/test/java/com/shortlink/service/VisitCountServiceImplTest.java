package com.shortlink.service;

import com.shortlink.mapper.ShortLinkMapper;
import com.shortlink.service.impl.VisitCountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VisitCountServiceImplTest {

    private ShortLinkMapper mapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisConnectionFactory connectionFactory;
    private RedisConnection connection;
    private VisitCountServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(ShortLinkMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        connectionFactory = mock(RedisConnectionFactory.class);
        connection = mock(RedisConnection.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        service = new VisitCountServiceImpl(mapper, redisTemplate);
    }

    @Test
    void flushesVisitCountsWithScanInsteadOfKeys() {
        Cursor<byte[]> cursor = mock(Cursor.class);
        when(connection.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("link:count:000001".getBytes(StandardCharsets.UTF_8));
        when(valueOperations.get("link:count:000001")).thenReturn("5");

        service.flushVisitCounts();

        verify(mapper).incrementVisitCount("000001", 5L);
        verify(redisTemplate).delete("link:count:000001");
    }
}
