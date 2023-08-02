package chuyong.springspigot

import chuyong.springspigot.scheduler.SchedulerService
import chuyong.springspigot.scheduler.SpigotScheduler
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler

@Configuration
@ConditionalOnClass(Bukkit::class)
class SpigotAutoConfiguration {

    @Autowired
    lateinit var applicationContext: ApplicationContext
    private var initialized = false

    @Bean
    fun scopeBeanFactoryPostProcessor(): BeanFactoryPostProcessor? {
        return ScopePostProcessor()
    }

//    @EventListener
//    fun onStartup(event: ContextRefreshedEvent) {
//        if (initialized) return
//        initialized = true
//        val beans: Collection<Listener> = applicationContext.getBeansOfType(
//            Listener::class.java
//        ).values
//        val eventService: EventService = applicationContext.getBean(EventService::class.java)
//        beans.forEach(Consumer { listener: Listener ->
//            eventService.registerEvents(
//                listener
//            )
//        })
//    }

    @Bean
    fun taskScheduler(
        scheduler: SchedulerService,
        @Value("\${spigot.scheduler.poolSize:1}") poolSize: Int,
    ): TaskScheduler? {
        return SpigotScheduler(scheduler)
    }

    @Bean(destroyMethod = "")
    fun serverBean(plugin: Plugin): Server? {
        return plugin.server
    }

    @Bean(destroyMethod = "")
    fun pluginBean(@Value("\${spigot.plugin}") pluginName: String): Plugin? {
        return Bukkit.getPluginManager().getPlugin(pluginName)
    }

    @Bean(destroyMethod = "")
    fun schedulerBean(server: Server): BukkitScheduler {
        return server.scheduler
    }
}
