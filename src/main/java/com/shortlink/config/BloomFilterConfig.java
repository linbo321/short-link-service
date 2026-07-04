package com.shortlink.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.shortlink.mapper.ShortLinkMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class BloomFilterConfig {
    private static final int EXPECTED_INSERTIONS = 1_000_000;
    private static final double FALSE_POSITIVE_PROBABILITY = 0.01D;

    private final ShortLinkMapper shortLinkMapper;
    private BloomFilter<CharSequence> bloomFilter;

    public BloomFilterConfig(ShortLinkMapper shortLinkMapper) {
        this.shortLinkMapper = shortLinkMapper;
    }

    @PostConstruct
    public void initialize() {
        bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_PROBABILITY
        );
        List<String> existingCodes = shortLinkMapper.findAllShortCodes();
        if (existingCodes != null) {
            existingCodes.stream().filter(code -> code != null && !code.isEmpty()).forEach(bloomFilter::put);
        }
    }

    public boolean mightContain(String shortCode) {
        return bloomFilter == null || bloomFilter.mightContain(shortCode);
    }

    public void put(String shortCode) {
        if (bloomFilter != null && shortCode != null && !shortCode.isEmpty()) {
            bloomFilter.put(shortCode);
        }
    }
}
