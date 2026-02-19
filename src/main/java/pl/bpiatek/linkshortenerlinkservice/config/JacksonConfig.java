package pl.bpiatek.linkshortenerlinkservice.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder
                // 1. Date/Time Support: Essential for objects like java.time.Instant
                .modules(new JavaTimeModule())

                // 2. Human-Readable Dates: Forces ISO-8601 strings (e.g., "2026-02-19T14:30:00Z")
                // instead of unreadable timestamp arrays [2026, 2, 19, 14, 30]
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

                // 3. Forward Compatibility: Prevents crashes if Kafka sends a JSON
                // payload with new fields your older Link object doesn't know about yet
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

                // 4. Payload Optimization: Drops null fields entirely to save Postgres JSONB
                // storage space and reduce Kafka network bandwidth
                .serializationInclusion(JsonInclude.Include.NON_NULL)

                .build();
    }

}
