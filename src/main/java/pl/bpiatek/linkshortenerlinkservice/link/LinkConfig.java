package pl.bpiatek.linkshortenerlinkservice.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import java.time.Clock;
import java.util.List;
import java.util.Set;

@Configuration
class LinkConfig {

    @Bean
    LinkRepository linkRepository(NamedParameterJdbcTemplate namedJdbcTemplate, Clock clock, OutboxRepository outboxRepository) {
        return new JdbcLinkRepository(namedJdbcTemplate, clock, outboxRepository);
    }

    @Bean
    OutboxRepository outboxRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, Clock clock) {
        return new OutboxRepository(jdbcTemplate, objectMapper, clock);
    }

    @Bean
    @ConditionalOnProperty(value = "app.scheduling.enable-sweeper", havingValue = "true", matchIfMissing = true)
    SweeperOutboxRelay outboxRelay(OutboxRepository outboxRepository,
                                   ObjectMapper objectMapper,
                                   LinkLifecycleKafkaProducer linkLifecycleKafkaProducer) {
        return new SweeperOutboxRelay(
                outboxRepository,
                objectMapper,
                linkLifecycleKafkaProducer);
    }

    @Bean
    LinkLifecycleKafkaProducer linkLifecycleKafkaProducer(
            @Value("${topic.link.lifecycle}") String topicName,
            KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate,
            Clock clock) {
        return new LinkLifecycleKafkaProducer(topicName, kafkaTemplate, clock);
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
    CustomShortUrlCreationStrategy customCodeCreationStrategy(LinkRepository linkRepository,
                                                              LinkMapper linkMapper,
                                                              ReservedWordsValidator reservedWordsValidator,
                                                              TransactionTemplate transactionTemplate) {
        return new CustomShortUrlCreationStrategy(
                linkRepository,
                linkMapper,
                reservedWordsValidator,
                transactionTemplate);
    }

    @Bean
    LinkCreatedKafkaProducer linkCreatedKafkaProducer(
            @Value("${topic.link.lifecycle}") String topicName,
            KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate) {
        return new LinkCreatedKafkaProducer(topicName, kafkaTemplate);
    }

    @Bean
    LinkUpdatedKafkaProducer linkUpdatedKafkaProducer(
            @Value("${topic.link.lifecycle}") String topicName,
            KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate) {
        return  new LinkUpdatedKafkaProducer(topicName, kafkaTemplate);
    }

    @Bean
    LinkDeletedKafkaProducer linkDeletedKafkaProducer(
            @Value("${topic.link.lifecycle}") String topicName,
            KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate,
            Clock clock) {
        return new LinkDeletedKafkaProducer(topicName, kafkaTemplate, clock);
    }

    @Bean
    FastTrackOutboxListener linkCreatedPublisher(LinkLifecycleKafkaProducer kafkaProducer,
                                                 OutboxRepository outboxRepository) {
        return new FastTrackOutboxListener(
                kafkaProducer,
                outboxRepository);
    }

    @Bean
    RandomShortUrlCreationStrategy randomCodeCreationStrategy(LinkRepository linkRepository,
                                                              LinkMapper linkMapper,
                                                              ShortUrlGenerator shortUrlGenerator,
                                                              TransactionTemplate transactionTemplate) {
        return new RandomShortUrlCreationStrategy(linkRepository, linkMapper, shortUrlGenerator, transactionTemplate);
    }

    @Bean
    LinkManipulationService linkManipulationService(LinkRepository linkRepository,
                                                    ApplicationEventPublisher eventPublisher,
                                                    Clock clock,
                                                    LinkMapper linkMapper,
                                                    @Value("${link.delete.deactivated.custom.older.than.days}") Integer days) {
        return new LinkManipulationService(linkRepository, eventPublisher, clock, linkMapper, days);
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
}
