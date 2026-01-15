package pl.bpiatek.linkshortenerlinkservice.link;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface LinkRepository {

    Link save(Link link);

    Optional<Link> findByShortUrl(String shortUrl);

    List<Link> findByUserId(String userId);

    void update(Link link);

    Optional<Link> findByIdAndUserId(Long id, String userId);

    void deleteByIdAndUserId(Long id, String userId);

    int deleteDeactivatedCustomLinksOlderThan(Instant cutoffDate);
}
