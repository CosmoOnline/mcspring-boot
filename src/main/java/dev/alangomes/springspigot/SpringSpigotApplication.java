package dev.alangomes.springspigot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("dev.alangomes.springspigot")
public class SpringSpigotApplication {
    @Bean
    public SpringSpigotAutoConfiguration autoConfiguration() {
        return new SpringSpigotAutoConfiguration();
    }
}
