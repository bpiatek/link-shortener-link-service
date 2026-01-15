package pl.bpiatek.linkshortenerlinkservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LinkShortenerLinkServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkShortenerLinkServiceApplication.class, args);
    }

}
