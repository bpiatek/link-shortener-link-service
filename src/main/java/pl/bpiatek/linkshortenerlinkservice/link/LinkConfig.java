package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Configuration
class LinkConfig {

    @Bean
    LinkRepository linkRepository(NamedParameterJdbcTemplate namedJdbcTemplate, Clock clock) {
        return new JdbcLinkRepository(namedJdbcTemplate, clock);
    }

    @Bean
    OutboxRepository outboxRepository(NamedParameterJdbcTemplate namedJdbcTemplate) {
        return new OutboxRepository(namedJdbcTemplate);
    }

    @Bean
    LinkLifecycleEventFactory linkLifecycleEventFactory(Clock clock) {
        return new LinkLifecycleEventFactory(clock);
    }

    @Bean
    @ConditionalOnProperty(value = "app.scheduling.enable-sweeper", havingValue = "true", matchIfMissing = true)
    SweeperOutboxScheduler outboxRelay(OutboxRepository outboxRepository,
                                       LinkLifecycleKafkaProducer linkLifecycleKafkaProducer,
                                       TransactionTemplate transactionTemplate) {
        return new SweeperOutboxScheduler(
                outboxRepository,
                linkLifecycleKafkaProducer,
                transactionTemplate);
    }

    @Bean
    LinkLifecycleKafkaProducer linkLifecycleKafkaProducer(
            @Value("${topic.link.lifecycle}") String topicName,
            KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate,
            LinkLifecycleEventFactory linkLifecycleEventFactory) {
        return new LinkLifecycleKafkaProducer(topicName, kafkaTemplate, linkLifecycleEventFactory);
    }

    @Bean
    LinkMapper linkMapper(@Value("${link.base.url}") String baseLinkUrl) {
        return new LinkMapper(baseLinkUrl);
    }

    @Bean
    ShortUrlGenerator shortUrlGenerator(@Value("${link.short.length}") int shortUrlLength) {
        return new ShortUrlGenerator(shortUrlLength);
    }

    @Bean
    ReservedWordsValidator reservedWordsValidator(@Value("${link.reserved-words}") Set<String> reservedWords) {
        return new ReservedWordsValidator(reservedWords);
    }

    @Bean
    CustomShortUrlCreationStrategy customCodeCreationStrategy(LinkMapper linkMapper,
                                                              ReservedWordsValidator reservedWordsValidator,
                                                              LinkTransactionalPersister linkTransactionalPersister) {
        return new CustomShortUrlCreationStrategy(
                linkMapper,
                reservedWordsValidator,
                linkTransactionalPersister);
    }

    @Bean
    FastTrackOutboxListener linkCreatedPublisher(LinkLifecycleKafkaProducer kafkaProducer,
                                                 OutboxRepository outboxRepository,
                                                 @Value("${app.outbox.fast-track-concurrency}") int maxConcurrency) {
        return new FastTrackOutboxListener(
                kafkaProducer,
                outboxRepository,
                maxConcurrency);
    }

    @Bean
    RandomShortUrlCreationStrategy randomCodeCreationStrategy(LinkMapper linkMapper,
                                                              ShortUrlGenerator shortUrlGenerator,
                                                              LinkTransactionalPersister linkTransactionalPersister) {
        return new RandomShortUrlCreationStrategy(linkMapper, shortUrlGenerator, linkTransactionalPersister);
    }

    @Bean
    LinkTransactionalPersister linkTransactionalPersister(LinkRepository linkRepository,
                                                          OutboxRepository outboxRepository,
                                                          LinkLifecycleEventFactory eventFactory,
                                                          TransactionTemplate transactionTemplate,
                                                          LinkMapper linkMapper) {
        return new LinkTransactionalPersister(
                linkRepository,
                outboxRepository,
                eventFactory,
                transactionTemplate,
                linkMapper
        );
    }

    @Bean
    LinkManipulationService linkManipulationService(LinkRepository linkRepository,
                                                    ApplicationEventPublisher eventPublisher,
                                                    Clock clock,
                                                    LinkMapper linkMapper,
                                                    @Value("${link.delete.deactivated.custom.older.than.days}") Integer days,
                                                    OutboxRepository outboxRepository,
                                                    LinkLifecycleEventFactory lifecycleEventFactory) {
        return new LinkManipulationService(
                linkRepository,
                eventPublisher,
                clock,
                linkMapper,
                days,
                outboxRepository,
                lifecycleEventFactory);
    }

    @Bean
    LinkRetriever linkRetriever(LinkRepository linkRepository, LinkMapper linkMapper) {
        return new LinkRetriever(linkRepository, linkMapper);
    }

    @Bean
    LinkFacade linkFacade(List<LinkCreationStrategy> strategyList,
                          ApplicationEventPublisher eventPublisher,
                          LinkManipulationService linkManipulationService,
                          LinkRetriever linkRetriever) {
        return new LinkFacade(strategyList, eventPublisher, linkManipulationService, linkRetriever);
    }

    @Bean
    LinkCleanupScheduler linkCleanupScheduler(LinkManipulationService linkManipulationService) {
        return new LinkCleanupScheduler(linkManipulationService);
    }

    @Bean
    RestClient vaultRestClient(RestClient.Builder builder,
                               @Value("${vault.address:http://vault.vault.svc.cluster.local:8200}") String vaultAddress) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        return builder
                .baseUrl(vaultAddress)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {})
                .build();
    }
}
