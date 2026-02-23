package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;
import pl.bpiatek.linkshortenerlinkservice.api.dto.CreateLinkResponse;

import java.util.Objects;
import java.util.UUID;

class LinkTransactionalPersister {

    private final LinkRepository linkRepository;
    private final OutboxRepository outboxRepository;
    private final LinkLifecycleEventFactory eventFactory;
    private final TransactionTemplate transactionTemplate;
    private final LinkMapper linkMapper;

    LinkTransactionalPersister(LinkRepository linkRepository,
                               OutboxRepository outboxRepository,
                               LinkLifecycleEventFactory eventFactory,
                               TransactionTemplate transactionTemplate,
                               LinkMapper linkMapper) {
        this.linkRepository = linkRepository;
        this.outboxRepository = outboxRepository;
        this.eventFactory = eventFactory;
        this.transactionTemplate = transactionTemplate;
        this.linkMapper = linkMapper;
    }


    public CreateLinkResponse persistAndPublish(Link linkToSave, ApplicationEventPublisher eventPublisher) {
        var savedLink = transactionTemplate.execute(status -> {
            var result = linkRepository.save(linkToSave);
            var eventId = UUID.randomUUID();

            var protobufEvent = eventFactory.createEvent(eventId, result, LinkEventType.LINK_CREATED);

            outboxRepository.saveEvent(
                    String.valueOf(result.id()),
                    LinkEventType.LINK_CREATED,
                    protobufEvent
            );

            eventPublisher.publishEvent(new LinkCreatedApplicationEvent(eventId, result));

            return result;
        });

        Objects.requireNonNull(savedLink, "Transaction completed but returned a null Link");

        return linkMapper.toCreateLinkResponse(savedLink);
    }
}
