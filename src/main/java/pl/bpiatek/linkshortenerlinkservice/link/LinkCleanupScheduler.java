package pl.bpiatek.linkshortenerlinkservice.link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

class LinkCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LinkCleanupScheduler.class);

    private final LinkManipulationService linkManipulationService;

    LinkCleanupScheduler(LinkManipulationService linkManipulationService) {
        this.linkManipulationService = linkManipulationService;
    }

    @Scheduled(cron = "${link.cleanup.cron:0 0 3 1 * ?}")
    public void cleanupDeactivatedVanityLinks() {
        log.info("Starting scheduled cleanup of deactivated vanity links.");
        try {
            int deletedCount = linkManipulationService.releaseDeactivatedVanityLinks();
            log.info("Finished cleanup. Released {} vanity links.", deletedCount);
        } catch (Exception e) {
            log.error("Error occurred during vanity link cleanup", e);
        }
    }
}
