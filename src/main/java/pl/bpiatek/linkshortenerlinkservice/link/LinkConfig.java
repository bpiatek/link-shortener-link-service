package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto;

import java.time.Clock;
import java.util.List;

@Configuration
class LinkConfig {

    @Bean
    LinkRepository linkRepository(NamedParameterJdbcTemplate namedJdbcTemplate, Clock clock) {
        return new JdbcLinkRepository(namedJdbcTemplate, clock);
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
    CustomShortUrlCreationStrategy customCodeCreationStrategy(LinkRepository linkRepository, LinkMapper linkMapper) {
        return new CustomShortUrlCreationStrategy(linkRepository, linkMapper);
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
            KafkaTemplate<String, LinkLifecycleEventProto.LinkLifecycleEvent> kafkaTemplate) {
        return new LinkDeletedKafkaProducer(topicName, kafkaTemplate);
    }

    @Bean
    LinkEventsPublisher linkCreatedPublisher(LinkCreatedKafkaProducer linkCreatedKafkaProducer,
                                             LinkUpdatedKafkaProducer linkUpdatedKafkaProducer,
                                             LinkDeletedKafkaProducer linkDeletedKafkaProducer) {
        return new LinkEventsPublisher(linkCreatedKafkaProducer, linkUpdatedKafkaProducer, linkDeletedKafkaProducer);
    }

    @Bean
    RandomShortUrlCreationStrategy randomCodeCreationStrategy(LinkRepository linkRepository,
                                                              LinkMapper linkMapper,
                                                              ShortUrlGenerator shortUrlGenerator) {
        return new RandomShortUrlCreationStrategy(linkRepository, linkMapper, shortUrlGenerator);
    }

    @Bean
    LinkManipulationService linkUpdateService(LinkRepository linkRepository, ApplicationEventPublisher eventPublisher, Clock clock, LinkMapper linkMapper) {
        return new LinkManipulationService(linkRepository, eventPublisher, clock, linkMapper);
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
}
