package chuyong.springspigot

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@ComponentScan("chuyong.springspigot")
@EnableAutoConfiguration
@EnableScheduling
@Import(DataSourceAutoConfiguration::class)
class SpringSpigotApplication
