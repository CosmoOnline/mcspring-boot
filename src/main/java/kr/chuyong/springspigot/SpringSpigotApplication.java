package kr.chuyong.springspigot;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan("kr.chuyong.springspigot")
@EnableAutoConfiguration
@EnableScheduling
public class SpringSpigotApplication {
    public SpringSpigotApplication(DataSourceAutoConfiguration configuration) {
        System.out.println("DB : " + configuration);
    }
}
