package com.shortlink.service;

public interface VisitCountService {
    void incrementVisitAsync(String shortCode);

    void deleteVisitCount(String shortCode);

    void flushVisitCounts();
}
