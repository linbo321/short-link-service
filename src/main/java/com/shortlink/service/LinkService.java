package com.shortlink.service;

import com.shortlink.dto.GenerateLinkRequest;
import com.shortlink.dto.LinkInfoResponse;
import com.shortlink.dto.LinkResponse;

import java.util.List;

public interface LinkService {
    LinkResponse generate(GenerateLinkRequest request, String baseUrl);

    String resolveOriginalUrl(String shortCode);

    LinkInfoResponse getInfo(String shortCode);

    List<LinkInfoResponse> listRecent(int limit);

    void delete(String shortCode);

    void flushVisitCounts();

    int cleanupExpiredLinks();
}
