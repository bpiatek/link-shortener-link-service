package pl.bpiatek.linkshortenerlinkservice.link;

import java.util.List;
import java.util.Optional;

interface LinkRepository {

    Link save(Link link);

    Optional<Link> findByShortUrl(String shortUrl);

    List<Link> findByUserId(String userId);
}

