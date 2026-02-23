package pl.bpiatek.linkshortenerlinkservice.link;

import java.util.UUID;

record LinkDeletedApplicationEvent(UUID outboxEventId, Link link) {
}
