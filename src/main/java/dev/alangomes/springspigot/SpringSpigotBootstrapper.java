package dev.alangomes.springspigot;

import dev.alangomes.springspigot.util.CompoundClassLoader;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class SpringSpigotBootstrapper extends JavaPlugin{
    private ConfigurableApplicationContext context;

    @SneakyThrows
    @Override
    public void onEnable() {
        val classLoader = new CompoundClassLoader(Thread.currentThread().getContextClassLoader(), getClassLoader());
        val pluginClasses = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(plugin -> plugin instanceof JavaPlugin && plugin.getClass().isAnnotationPresent(EnableSpringSpigotSupport.class))
                .toList();
        context = initialize(this, classLoader, new SpringApplicationBuilder(), pluginClasses);

        for(String names : context.getBeanDefinitionNames()){
            System.out.println(names);
        }
    }

    @Override
    public void onDisable() {
        context.close();
        context = null;
    }

    public static ConfigurableApplicationContext initialize(JavaPlugin plugin, ClassLoader classLoader, SpringApplicationBuilder builder, List<Plugin> plugins) throws ExecutionException, InterruptedException {
        val executor = Executors.newSingleThreadExecutor();
        try {
            Future<ConfigurableApplicationContext> contextFuture = executor.submit(() -> {
                Thread.currentThread().setContextClassLoader(classLoader);

                val pluginClassesList = plugins.stream().map(Plugin::getClass).toList();
                val pluginClassesArray = pluginClassesList.toArray(new Class[0]);

                val props = new Properties();
                try {
                    props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
                } catch (Exception ignored) {
                }

                if (builder.application().getResourceLoader() == null) {
                    val loader = new DefaultResourceLoader(classLoader);
                    builder.resourceLoader(loader);
                }
                return builder
                        .properties(props)
                        .initializers(new SpringSpigotInitializer(plugin))
                        .child(pluginClassesArray)
                        .parent(SpringSpigotApplication.class)
                        .run();
            });
            return contextFuture.get();
        } finally {
            executor.shutdown();
        }
    }


}
