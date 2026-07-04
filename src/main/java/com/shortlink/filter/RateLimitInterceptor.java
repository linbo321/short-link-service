package com.shortlink.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortlink.common.ErrorCode;
import com.shortlink.common.Result;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private static final int WINDOW_SECONDS = 60;
    private static final int LIMIT = 10;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !"/api/link/generate".equals(request.getRequestURI())) {
            return true;
        }

        String key = "link:rate:" + clientIp(request);
        long now = System.currentTimeMillis();
        long windowStart = now - TimeUnit.SECONDS.toMillis(WINDOW_SECONDS);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        Long count = redisTemplate.opsForZSet().zCard(key);
        if (count != null && count >= LIMIT) {
            response.setStatus(ErrorCode.TOO_MANY_REQUESTS.getCode());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.fail(ErrorCode.TOO_MANY_REQUESTS.getCode(), ErrorCode.TOO_MANY_REQUESTS.getMessage())
            ));
            return false;
        }
        redisTemplate.opsForZSet().add(key, now + ":" + UUID.randomUUID(), now);
        redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
