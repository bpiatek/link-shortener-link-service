package pl.bpiatek.linkshortenerlinkservice.link;

import java.util.UUID;

record LinkCreatedApplicationEvent(UUID outboxEventId, Link link) {
}
