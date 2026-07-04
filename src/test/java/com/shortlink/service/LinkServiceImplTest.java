package com.shortlink.service;

import com.shortlink.dto.GenerateLinkRequest;
import com.shortlink.dto.LinkInfoResponse;
import com.shortlink.dto.LinkResponse;
import com.shortlink.entity.ShortLink;
import com.shortlink.mapper.ShortLinkMapper;
import com.shortlink.service.VisitCountService;
import com.shortlink.service.impl.LinkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkServiceImplTest {

    private ShortLinkMapper mapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private VisitCountService visitCountService;
    private LinkServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(ShortLinkMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        visitCountService = mock(VisitCountService.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new LinkServiceImpl(mapper, redisTemplate, visitCountService, Collections::emptyList);
    }

    @Test
    void reusesExistingUnexpiredShortLinkForSameOriginalUrl() {
        ShortLink existing = new ShortLink();
        existing.setId(9L);
        existing.setShortCode("000009");
        existing.setOriginalUrl("https://example.com/a");
        existing.setVisitCount(3);
        existing.setCreateTime(LocalDateTime.of(2026, 7, 4, 12, 0));
        existing.setExpireTime(LocalDateTime.now().plusHours(1));
        when(mapper.findLatestByOriginalUrl("https://example.com/a")).thenReturn(existing);

        GenerateLinkRequest request = new GenerateLinkRequest();
        request.setUrl("https://example.com/a");
        request.setExpireHours(24);

        LinkResponse response = service.generate(request, "http://s.cn");

        assertThat(response.getShortCode()).isEqualTo("000009");
        assertThat(response.getShortUrl()).isEqualTo("http://s.cn/000009");
        assertThat(response.getOriginalUrl()).isEqualTo("https://example.com/a");
        verify(mapper, never()).insert(any());
    }

    @Test
    void createsNewShortLinkWhenExistingOneIsExpired() {
        ShortLink expired = new ShortLink();
        expired.setShortCode("000123");
        expired.setOriginalUrl("https://example.com/b");
        expired.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(mapper.findLatestByOriginalUrl("https://example.com/b")).thenReturn(expired);

        GenerateLinkRequest request = new GenerateLinkRequest();
        request.setUrl("https://example.com/b");
        request.setExpireHours(2);

        ArgumentCaptor<ShortLink> insertCaptor = ArgumentCaptor.forClass(ShortLink.class);
        ArgumentCaptor<ShortLink> updateCaptor = ArgumentCaptor.forClass(ShortLink.class);
        when(mapper.insert(insertCaptor.capture())).thenAnswer(invocation -> {
            insertCaptor.getValue().setId(1_000_000L);
            return 1;
        });
        when(mapper.updateById(updateCaptor.capture())).thenReturn(1);

        LinkResponse response = service.generate(request, "http://s.cn/");

        assertThat(response.getShortCode()).isEqualTo("004c92");
        assertThat(response.getShortUrl()).isEqualTo("http://s.cn/004c92");
        assertThat(response.getExpireTime()).isAfter(LocalDateTime.now().plusMinutes(110));
        assertThat(updateCaptor.getValue().getShortCode()).isEqualTo("004c92");
        verify(valueOperations).set(eq("link:004c92"), eq("https://example.com/b"), eq(1L), eq(TimeUnit.HOURS));
    }

    @Test
    void listsRecentShortLinksForPageReload() {
        ShortLink first = new ShortLink();
        first.setShortCode("000006");
        first.setOriginalUrl("https://example.org/tutorial");
        first.setVisitCount(0);
        first.setCreateTime(LocalDateTime.of(2026, 7, 4, 14, 0));
        first.setExpireTime(LocalDateTime.of(2026, 7, 5, 14, 0));

        ShortLink second = new ShortLink();
        second.setShortCode("000005");
        second.setOriginalUrl("https://example.com");
        second.setVisitCount(2);
        second.setCreateTime(LocalDateTime.of(2026, 7, 4, 13, 0));
        second.setExpireTime(null);

        when(mapper.findRecent(6)).thenReturn(Arrays.asList(first, second));

        List<LinkInfoResponse> recent = service.listRecent(6);

        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).getShortCode()).isEqualTo("000006");
        assertThat(recent.get(0).getOriginalUrl()).isEqualTo("https://example.org/tutorial");
        assertThat(recent.get(1).getShortCode()).isEqualTo("000005");
        assertThat(recent.get(1).getVisitCount()).isEqualTo(2);
    }

    @Test
    void delegatesVisitCountingWhenResolvingCachedLink() {
        when(valueOperations.get("link:000001")).thenReturn("https://example.com/cached");

        String originalUrl = service.resolveOriginalUrl("000001");

        assertThat(originalUrl).isEqualTo("https://example.com/cached");
        verify(visitCountService).incrementVisitAsync("000001");
    }
}
