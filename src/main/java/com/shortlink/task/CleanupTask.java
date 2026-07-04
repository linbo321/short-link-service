package com.shortlink.task;

import com.shortlink.service.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanupTask {
    private static final Logger log = LoggerFactory.getLogger(CleanupTask.class);
    private final LinkService linkService;

    public CleanupTask(LinkService linkService) {
        this.linkService = linkService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredLinks() {
        int deleted = linkService.cleanupExpiredLinks();
        log.info("Cleaned {} expired short links", deleted);
    }

    @Scheduled(cron = "0 */10 * * * ?")
    public void flushVisitCounts() {
        linkService.flushVisitCounts();
    }
}
