package pl.bpiatek.linkshortenerlinkservice.link;

import java.util.UUID;

record LinkUpdatedApplicationEvent(UUID outboxEventId, Link link) {
}
