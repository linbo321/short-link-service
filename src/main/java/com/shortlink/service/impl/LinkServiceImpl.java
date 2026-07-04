package com.shortlink.service.impl;

import com.shortlink.common.BusinessException;
import com.shortlink.common.ErrorCode;
import com.shortlink.config.BloomFilterConfig;
import com.shortlink.dto.GenerateLinkRequest;
import com.shortlink.dto.LinkInfoResponse;
import com.shortlink.dto.LinkResponse;
import com.shortlink.entity.ShortLink;
import com.shortlink.mapper.ShortLinkMapper;
import com.shortlink.service.LinkService;
import com.shortlink.service.VisitCountService;
import com.shortlink.util.Base62Util;
import com.shortlink.util.UrlValidator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class LinkServiceImpl implements LinkService {
    private static final int SHORT_CODE_LENGTH = 6;
    private static final String LINK_CACHE_PREFIX = "link:";

    private final ShortLinkMapper shortLinkMapper;
    private final StringRedisTemplate redisTemplate;
    private final VisitCountService visitCountService;
    private final Supplier<List<String>> shortCodeSupplier;
    private final BloomFilterConfig bloomFilterConfig;

    @Autowired
    public LinkServiceImpl(ShortLinkMapper shortLinkMapper,
                           StringRedisTemplate redisTemplate,
                           VisitCountService visitCountService,
                           BloomFilterConfig bloomFilterConfig) {
        this.shortLinkMapper = shortLinkMapper;
        this.redisTemplate = redisTemplate;
        this.visitCountService = visitCountService;
        this.shortCodeSupplier = shortLinkMapper::findAllShortCodes;
        this.bloomFilterConfig = bloomFilterConfig;
    }

    public LinkServiceImpl(ShortLinkMapper shortLinkMapper,
                           StringRedisTemplate redisTemplate,
                           VisitCountService visitCountService) {
        this(shortLinkMapper, redisTemplate, visitCountService, shortLinkMapper::findAllShortCodes);
    }

    public LinkServiceImpl(ShortLinkMapper shortLinkMapper,
                           StringRedisTemplate redisTemplate,
                           VisitCountService visitCountService,
                           Supplier<List<String>> shortCodeSupplier) {
        this.shortLinkMapper = shortLinkMapper;
        this.redisTemplate = redisTemplate;
        this.visitCountService = visitCountService;
        this.shortCodeSupplier = shortCodeSupplier;
        this.bloomFilterConfig = null;
    }

    @Override
    public LinkResponse generate(GenerateLinkRequest request, String baseUrl) {
        if (request == null || !UrlValidator.isValidHttpUrl(request.getUrl())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入合法的URL地址");
        }

        ShortLink existing = shortLinkMapper.findLatestByOriginalUrl(request.getUrl());
        if (existing != null && !existing.isExpired() && StringUtils.hasText(existing.getShortCode())) {
            return toLinkResponse(existing, baseUrl);
        }

        LocalDateTime now = LocalDateTime.now();
        ShortLink shortLink = new ShortLink();
        shortLink.setOriginalUrl(request.getUrl().trim());
        shortLink.setVisitCount(0);
        shortLink.setExpireTime(toExpireTime(request.getExpireHours(), now));
        shortLink.setCreateTime(now);
        shortLink.setUpdateTime(now);

        shortLinkMapper.insert(shortLink);
        if (shortLink.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "短码生成失败");
        }

        String shortCode = Base62Util.encodeToFixedLength(shortLink.getId(), SHORT_CODE_LENGTH);
        shortLink.setShortCode(shortCode);
        shortLink.setUpdateTime(LocalDateTime.now());
        shortLinkMapper.updateById(shortLink);
        cacheOriginalUrl(shortCode, shortLink.getOriginalUrl());
        if (bloomFilterConfig != null) {
            bloomFilterConfig.put(shortCode);
        }
        return toLinkResponse(shortLink, baseUrl);
    }

    @Override
    public String resolveOriginalUrl(String shortCode) {
        validateShortCode(shortCode);
        if (bloomFilterConfig != null && !bloomFilterConfig.mightContain(shortCode)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String cacheKey = LINK_CACHE_PREFIX + shortCode;
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cachedUrl)) {
            visitCountService.incrementVisitAsync(shortCode);
            return cachedUrl;
        }

        ShortLink shortLink = shortLinkMapper.findByShortCode(shortCode);
        if (shortLink == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (shortLink.isExpired()) {
            redisTemplate.delete(cacheKey);
            throw new BusinessException(ErrorCode.GONE);
        }
        cacheOriginalUrl(shortCode, shortLink.getOriginalUrl());
        visitCountService.incrementVisitAsync(shortCode);
        return shortLink.getOriginalUrl();
    }

    @Override
    public LinkInfoResponse getInfo(String shortCode) {
        validateShortCode(shortCode);
        ShortLink shortLink = shortLinkMapper.findByShortCode(shortCode);
        if (shortLink == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return toInfoResponse(shortLink);
    }

    @Override
    public List<LinkInfoResponse> listRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<ShortLink> links = shortLinkMapper.findRecent(safeLimit);
        return links.stream().map(this::toInfoResponse).toList();
    }

    private LinkInfoResponse toInfoResponse(ShortLink shortLink) {
        LinkInfoResponse response = new LinkInfoResponse();
        response.setShortCode(shortLink.getShortCode());
        response.setOriginalUrl(shortLink.getOriginalUrl());
        response.setVisitCount(shortLink.getVisitCount() == null ? 0 : shortLink.getVisitCount());
        response.setCreateTime(shortLink.getCreateTime());
        response.setExpireTime(shortLink.getExpireTime());
        response.setExpired(shortLink.isExpired());
        return response;
    }

    @Override
    public void delete(String shortCode) {
        validateShortCode(shortCode);
        ShortLink shortLink = shortLinkMapper.findByShortCode(shortCode);
        if (shortLink == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        shortLinkMapper.deleteById(shortLink.getId());
        redisTemplate.delete(LINK_CACHE_PREFIX + shortCode);
        visitCountService.deleteVisitCount(shortCode);
    }

    @Override
    public void flushVisitCounts() {
        visitCountService.flushVisitCounts();
    }

    @Override
    public int cleanupExpiredLinks() {
        return shortLinkMapper.deleteExpiredBeforeRetention();
    }

    public List<String> loadExistingShortCodes() {
        return shortCodeSupplier.get();
    }

    private void cacheOriginalUrl(String shortCode, String originalUrl) {
        redisTemplate.opsForValue().set(LINK_CACHE_PREFIX + shortCode, originalUrl, 1L, TimeUnit.HOURS);
    }

    private LinkResponse toLinkResponse(ShortLink shortLink, String baseUrl) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        LinkResponse response = new LinkResponse();
        response.setShortCode(shortLink.getShortCode());
        response.setShortUrl(normalizedBaseUrl + "/" + shortLink.getShortCode());
        response.setOriginalUrl(shortLink.getOriginalUrl());
        response.setExpireTime(shortLink.getExpireTime());
        response.setCreateTime(shortLink.getCreateTime());
        return response;
    }

    private LocalDateTime toExpireTime(Integer expireHours, LocalDateTime now) {
        if (expireHours == null || expireHours <= 0) {
            return null;
        }
        return now.plusHours(expireHours);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private void validateShortCode(String shortCode) {
        if (!StringUtils.hasText(shortCode) || !shortCode.matches("[0-9a-zA-Z]{1,8}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的短链接格式");
        }
    }

}
