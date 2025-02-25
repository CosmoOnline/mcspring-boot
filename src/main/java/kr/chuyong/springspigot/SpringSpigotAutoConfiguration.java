package kr.chuyong.springspigot;

import kr.chuyong.springspigot.context.Context;
import kr.chuyong.springspigot.event.EventService;
import kr.chuyong.springspigot.scope.SenderContextScope;
import kr.chuyong.springspigot.util.scheduler.SchedulerService;
import kr.chuyong.springspigot.util.scheduler.SpigotScheduler;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfiguration;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

@Configuration
@ConditionalOnClass({Bukkit.class})
class SpringSpigotAutoConfiguration {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private boolean initialized = false;

    @Bean
    public static BeanFactoryPostProcessor scopeBeanFactoryPostProcessor(SenderContextScope scope) {
        return new ScopePostProcessor(scope);
    }

    @EventListener
    void onStartup(ContextRefreshedEvent event) {
        if (initialized) return;
        initialized = true;
        val beans = applicationContext.getBeansOfType(Listener.class).values();
        val eventService = applicationContext.getBean(EventService.class);
        beans.forEach(eventService::registerEvents);
    }

    @Bean
    //@Scope(SCOPE_SINGLETON)
    //@ConditionalOnBean(SchedulingConfiguration.class)
    public TaskScheduler taskScheduler(Context context, SchedulerService scheduler, @Value("${spigot.scheduler.poolSize:1}") int poolSize) {
        return new SpigotScheduler(scheduler, context);
    }

    @Bean(destroyMethod = "")
    Server serverBean(Plugin plugin) {
        return plugin.getServer();
    }

    @Bean(destroyMethod = "")
    Plugin pluginBean(@Value("${spigot.plugin}") String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName);
    }

    @Bean(destroyMethod = "")
    BukkitScheduler schedulerBean(Server server) {
        return server.getScheduler();
    }
}
