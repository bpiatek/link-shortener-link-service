package pl.bpiatek.linkshortenerlinkservice.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import io.micrometer.observation.ObservationRegistry;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import pl.bpiatek.contracts.link.LinkLifecycleEventProto.LinkLifecycleEvent;

import java.util.Map;

@Configuration
@EnableKafka
class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    ProducerFactory<String, LinkLifecycleEvent> producerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);
        props.putIfAbsent(AbstractKafkaSchemaSerDeConfig.USE_LATEST_VERSION, true);

        var registryUrl = kafkaProperties.getProperties().get("schema.registry.url");
        if (registryUrl != null) {
            props.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, registryUrl);
        }

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    KafkaTemplate<String, LinkLifecycleEvent> kafkaTemplate(ObservationRegistry observationRegistry) {
        var template = new KafkaTemplate<>(producerFactory());

        template.setObservationEnabled(true);
        template.setObservationRegistry(observationRegistry);

        return template;
    }
}
