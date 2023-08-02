package kr.chuyong.springspigot;

import kr.chuyong.springspigot.annotation.CommandAdvice;
import kr.chuyong.springspigot.annotation.CommandController;
import kr.chuyong.springspigot.annotation.CommandMapping;
import kr.chuyong.springspigot.commands.BukkitCommandHandler;
import kr.chuyong.springspigot.external.ExternalDependencyProvider;
import kr.chuyong.springspigot.util.CompoundClassLoader;
import kr.chuyong.springspigot.util.YamlPropertiesFactory;
import kr.hqservice.framework.bukkit.core.netty.NettyModule;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StopWatch;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public final class SpringSpigotBootstrapper extends JavaPlugin {
    public static ClassLoader classLoader = null;
    private final Logger logger = LoggerFactory.getLogger(SpringSpigotBootstrapper.class);
    private ConfigurableApplicationContext context;

    public SpringSpigotBootstrapper() {
        val parentClassLoader = Thread.currentThread().getContextClassLoader();
        val mainClassLoader = getClassLoader();
        val compoundLoader = new CompoundClassLoader(mainClassLoader, parentClassLoader);
        Thread.currentThread().setContextClassLoader(compoundLoader);
        classLoader = compoundLoader;
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

    @SneakyThrows
    @Override
    public void onEnable() {
        loadSpringSpigot();
    }

    @SneakyThrows
    public void loadSpringSpigot() {
        StopWatch watch = new StopWatch();
        watch.start();
        Bukkit.getConsoleSender().sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Initiated...");
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
                logger.info("Disabling plugin " + plugin.getName() + " To load from SpringSpigot..");
                Bukkit.getPluginManager().disablePlugin(plugin);

                ClassLoader newLoader = getClassLoader(getClassLoader(), file.toURI().toURL());
                classLoaders.add(newLoader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
        logger.info("Registering SpringSpigot Supported Plugins Completed");

        combinedLoader = new CompoundClassLoader(classLoaders);

        val ctx = new AnnotationConfigApplicationContext();
        ctx.setResourceLoader(new DefaultResourceLoader(combinedLoader));
        ctx.setClassLoader(combinedLoader);

        ctx.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("main-yaml", YamlPropertiesFactory.loadYamlIntoProperties(new FileSystemResource("application.yml"))));
        ctx.getEnvironment().getPropertySources().forEach((ptx) -> System.out.println(ptx.getClass().getName() + " with " + ptx.getProperty("spring.datasource.url")));

        val initializer = new SpringSpigotInitializer(this);
        initializer.initialize(ctx);
        ctx.register(SpringSpigotApplication.class);

        CompoundClassLoader finalCombinedLoader1 = combinedLoader;
        pluginClasses.forEach(pluginzz -> {
            val targetClazz = AopUtils.getTargetClass(pluginzz);
            try {
                val dynamicClazz = Class.forName(targetClazz.getName(), true, finalCombinedLoader1);
                ctx.scan(dynamicClazz.getPackageName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        ctx.refresh();
        context = ctx;

        val commandAdvices = context.getBeansWithAnnotation(CommandAdvice.class);
        commandAdvices.forEach((t, ob) -> {
            BukkitCommandHandler.registerAdvices(ob);
        });

        val commandControllers = context.getBeansWithAnnotation(CommandController.class);
        commandControllers.forEach((t, ob) -> {
            BukkitCommandHandler.registerCommands(ob);
        });

        watch.stop();
        Bukkit.getConsoleSender().sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Finished. Elapsed Time: " + watch.getTotalTimeMillis() + "ms");
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


}
