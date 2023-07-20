package dev.alangomes.springspigot;

import dev.alangomes.springspigot.annotation.CommandMapping;
import dev.alangomes.springspigot.commands.BukkitCommandHandler;
import dev.alangomes.springspigot.util.CompoundClassLoader;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

public final class SpringSpigotBootstrapper extends JavaPlugin {
    private Logger logger = LoggerFactory.getLogger(SpringSpigotBootstrapper.class);
    private ConfigurableApplicationContext context;
    public static ClassLoader classLoader = null;

    public SpringSpigotBootstrapper() {
        val parentClassLoader = Thread.currentThread().getContextClassLoader();
        val mainClassLoader = getClassLoader();
        val compoundLoader = new CompoundClassLoader(mainClassLoader, parentClassLoader);
        Thread.currentThread().setContextClassLoader(compoundLoader);
        classLoader = compoundLoader;
    }

    @SneakyThrows
    @Override
    public void onEnable() {
        val classLoaders = new ArrayList<ClassLoader>();
        classLoaders.add(Thread.currentThread().getContextClassLoader());
        classLoaders.add(getClassLoader());

        val pluginClasses = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(plugin -> plugin instanceof JavaPlugin && plugin.getClass().isAnnotationPresent(EnableSpringSpigotSupport.class))
                .toList();



        CompoundClassLoader combinedLoader = new CompoundClassLoader(classLoaders);
        Thread.currentThread().setContextClassLoader(combinedLoader);

        Class javaClassLoader = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
        Field f = javaClassLoader.getDeclaredField("file");
        f.setAccessible(true);

        logger.info("Registering SpringSpigot Supported Plugins...");
        pluginClasses.forEach(plugin -> {
            try {
                File file = (File) f.get(plugin.getClass().getClassLoader());
                logger.info("Disabling plugin " + plugin.getName() +" To load from SpringSpigot..");
                Bukkit.getPluginManager().disablePlugin(plugin);

                ClassLoader newLoader = getClassLoader(getClassLoader(), file.toURI().toURL());
                classLoaders.add( newLoader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
        logger.info("Registering SpringSpigot Supported Plugins Completed");

        combinedLoader = new CompoundClassLoader(classLoaders);



        val props = new Properties();
        try {
            props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
        } catch (Exception ignored) {
        }

        val ctx = new AnnotationConfigApplicationContext();
        ctx.setResourceLoader(new DefaultResourceLoader(combinedLoader));
        ctx.setClassLoader(combinedLoader);

        val initializer = new SpringSpigotInitializer(this);
        initializer.initialize(ctx);
        ctx.scan("dev.alangomes.springspigot");

        CompoundClassLoader finalCombinedLoader1 = combinedLoader;
        pluginClasses.forEach(pluginzz -> {
            val targetClazz = AopUtils.getTargetClass(pluginzz);
            try{
                val dynamicClazz = Class.forName(targetClazz.getName(), true, finalCombinedLoader1);
                ctx.scan(dynamicClazz.getPackageName());
            }catch( Exception ex ) {
                ex.printStackTrace();
            }
        });
        ctx.refresh();
        context = ctx;

        val commandControllers = context.getBeansWithAnnotation(CommandController.class);
        commandControllers.forEach((t, ob) -> {
            BukkitCommandHandler.registerCommands(ob);
        });
    }

    @Override
    public void onDisable() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    private Stream<Method> getCommandMethods(Object obj) {
        val target = AopUtils.getTargetClass(obj);
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(target))
                .filter(method ->
                        method.isAnnotationPresent(CommandMapping.class)
                );
    }

    public static ClassLoader getClassLoader(ClassLoader parent, URL url) {
        try {
          //  Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//            if (!method.isAccessible()) {
//                method.setAccessible(true);
//            }
            URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, parent);
          //  method.invoke(classLoader, url);
            return classLoader;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
