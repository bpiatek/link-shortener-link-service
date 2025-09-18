package pl.bpiatek.linkshortenerlinkservice.link;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
    RandomShortUrlCreationStrategy randomCodeCreationStrategy(LinkRepository linkRepository,
                                                              LinkMapper linkMapper,
                                                              ShortUrlGenerator shortUrlGenerator) {
        return new RandomShortUrlCreationStrategy(linkRepository, linkMapper, shortUrlGenerator);
    }

    @Bean
    LinkFacade linkFacade(List<LinkCreationStrategy> strategyList) {
        return new LinkFacade(strategyList);
    }
}
