package dev.alangomes.springspigot;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ComponentScan("dev.alangomes.springspigot")
@EnableAutoConfiguration
public class SpringSpigotApplication {
    public SpringSpigotApplication(DataSourceAutoConfiguration configuration){
        System.out.println("DB : " + configuration);
    }
}
