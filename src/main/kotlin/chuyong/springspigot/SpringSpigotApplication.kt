package chuyong.springspigot

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@ComponentScan("chuyong.springspigot")
@EnableAutoConfiguration
@EnableScheduling
class SpringSpigotApplication(
    private val db: DataSourceAutoConfiguration,
) {
    init {
        println(db)
    }
}
