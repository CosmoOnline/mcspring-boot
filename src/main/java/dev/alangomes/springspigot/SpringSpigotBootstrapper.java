package dev.alangomes.springspigot;

import api.cosmoage.bukkit.commands.$BukkitCommandHandler;
import api.cosmoage.global.GlobalCommandHandler;
import api.cosmoage.global.commandannotation.CosmoCMD;
import dev.alangomes.springspigot.util.CompoundClassLoader;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public final class SpringSpigotBootstrapper extends JavaPlugin{
    private ConfigurableApplicationContext context;

    @SneakyThrows
    @Override
    public void onEnable() {
        val logger = LoggerFactory.getLogger(SpringSpigotBootstrapper.class);
        val classLoaders = new ArrayList<ClassLoader>();
        classLoaders.add(Thread.currentThread().getContextClassLoader());
        classLoaders.add(getClassLoader());
        val pluginClasses = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(plugin -> plugin instanceof JavaPlugin && plugin.getClass().isAnnotationPresent(EnableSpringSpigotSupport.class))
                .toList();
        context = initialize(this, classLoaders, new SpringApplicationBuilder(SpringSpigotApplication.class), pluginClasses);

        val commandControllers = context.getBeansWithAnnotation(CommandController.class);
        //$registerCommands(CosmoCMD ano, Method mtd, Object cl)
        val handlerMethod = $BukkitCommandHandler.class.getDeclaredMethod("$registerCommands", CosmoCMD.class, Method.class, Object.class);
        handlerMethod.setAccessible(true);



//        commandControllers.forEach((t, ob) -> {
//            val test = getCommandMethods(ob);
//            getCommandMethods(ob).forEach(method -> {
//                CosmoCMD at = method.getAnnotation(CosmoCMD.class);
//                if (at != null) {
//                    CosmoCMD ano = at;
//                    try{
//                        handlerMethod.invoke(null, ano, method, ob);
//                    }catch(Exception ex){
//                        ex.printStackTrace();
//                    }
//                }
//            });
//        });


    }

    @Override
    public void onDisable() {
        if(context != null) {
            context.close();
            context = null;
        }
    }

    public static ConfigurableApplicationContext initialize(JavaPlugin plugin, List<ClassLoader> classLoaders, SpringApplicationBuilder builder, List<Plugin> pluginClasses) throws ExecutionException, InterruptedException {
        val executor = Executors.newSingleThreadExecutor();
        try {
            Future<ConfigurableApplicationContext> contextFuture = executor.submit(() -> {
                classLoaders.addAll(pluginClasses.stream().map(pl -> ((JavaPlugin)pl).getClass().getClassLoader()).toList());
                CompoundClassLoader loader = new CompoundClassLoader(classLoaders);
                Thread.currentThread().setContextClassLoader(loader);


                val props = new Properties();
                try {
                    props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
                } catch (Exception ignored) {
                }

                val ctx = new AnnotationConfigApplicationContext();
                ctx.setResourceLoader(new DefaultResourceLoader(loader));
                val initializer = new SpringSpigotInitializer(plugin);
                initializer.initialize(ctx);

                ctx.register(SpringSpigotApplication.class);

                ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(CommandController.class));

                pluginClasses.forEach(pluginzz -> {
                    ctx.scan(pluginzz.getClass().getPackageName());
//                    Set<BeanDefinition> beans = scanner.findCandidateComponents(pluginzz.getClass().getPackageName());
//                    for (BeanDefinition bean : beans) {
//                        try{
//                            ctx.register(Class.forName(bean.getBeanClassName()));
//                        }catch(Exception ex){
//                            ex.printStackTrace();
//                        }
//                    }
                });

                ctx.refresh();
                return ctx;
            });
            return contextFuture.get();
        } finally {
            executor.shutdown();
        }
    }

    private Stream<Method> getCommandMethods(Object obj) {
        val target = AopUtils.getTargetClass(obj);
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(target))
                .filter(method -> {
                    val annotationClass = AopUtils.getTargetClass(CosmoCMD.class);
                    System.out.println("MEeasdf");
                    for (Annotation annotation : method.getAnnotations()) {
                        System.out.println("ME" + annotation);
                        System.out.println("PRESENT " + method.isAnnotationPresent((Class<? extends Annotation>) annotationClass));
                    }

                    return Arrays.stream(method.getAnnotations()).anyMatch(annotation -> annotation.annotationType().equals(CosmoCMD.class));
                });
    }


}
