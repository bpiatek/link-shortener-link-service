package pl.bpiatek.linkshortenerlinkservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        SecurityConfig.class,
        GatewayHeaderFilter.class,
        ActuatorSecurityFilterChain.class,
        PrometheusSecurityFilterChain.class
})
public class TestSecurityConfiguration {
}
