package chuyong.springspigot

import chuyong.springspigot.command.BukkitCommandHandler
import chuyong.springspigot.command.annotation.CommandAdvice
import chuyong.springspigot.command.annotation.CommandController
import chuyong.springspigot.config.ConfigurationPropertySource
import chuyong.springspigot.event.EventService
import chuyong.springspigot.util.CompoundClassLoader
import chuyong.springspigot.util.YamlPropertiesFactory
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.util.StopWatch
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

class SpringSpigotBootstrapper : JavaPlugin() {
    private val masterClassLoader = CompoundClassLoader(Thread.currentThread().contextClassLoader, classLoader)
    private val logger = LoggerFactory.getLogger(SpringSpigotBootstrapper::class.java)
    lateinit var context: AnnotationConfigApplicationContext
    private val springExecutor = Executors.newSingleThreadExecutor()

    override fun onEnable() {
        loadSpringSpigot()
    }

    private fun loadSpringSpigot() {
        val watch = StopWatch()
        watch.start()
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Initiated...")

        val classLoaders = ArrayList<ClassLoader>()
        classLoaders.add(masterClassLoader)
      //  classLoaders.add(classLoader)

        val pluginClasses = Arrays.stream(Bukkit.getPluginManager().plugins)
            .filter { plugin: Plugin ->
                plugin is JavaPlugin && plugin.javaClass.isAnnotationPresent(
                    EnableSpringSpigotSupport::class.java
                )
            }
            .toList()

        val javaClassLoader = Class.forName("org.bukkit.plugin.java.PluginClassLoader")
        val f = javaClassLoader.getDeclaredField("file")
        f.isAccessible = true
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading Spring-Spigot added plugins...")
        pluginClasses.forEach(Consumer { plugin: Plugin ->
            try {
                val file = f[plugin.javaClass.classLoader] as File
                logger.info("Disabling plugin " + plugin.name + " To load from SpringSpigot..")
                Bukkit.getPluginManager().disablePlugin(plugin)
                val newLoader = URLClassLoader(arrayOf(file.toURI().toURL()), masterClassLoader)
                classLoaders.add(newLoader)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        })
        val combinedLoader = CompoundClassLoader(classLoaders)
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lBaking Custom ClassLoader Completed...")


        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lLoading SpringBoot...")
        CompletableFuture.runAsync({
            Thread.currentThread().contextClassLoader = masterClassLoader
            context = AnnotationConfigApplicationContext()

            context.apply {
                setResourceLoader(DefaultResourceLoader(combinedLoader))
                classLoader = combinedLoader
                environment.propertySources.addLast(
                    PropertiesPropertySource(
                        "main-yaml",
                        YamlPropertiesFactory.loadYamlIntoProperties(FileSystemResource("application.yml"))!!
                    )
                )
                val propertySources: MutablePropertySources = this.environment.propertySources
                propertySources.addLast(ConfigurationPropertySource(config))
                val props = Properties()
                props["spigot.plugin"] = name
                propertySources.addLast(PropertiesPropertySource("spring-bukkit", props))


                register(SpringSpigotApplication::class.java)

                pluginClasses.forEach(Consumer { pluginzz: Plugin ->
                    val targetClazz = AopUtils.getTargetClass(pluginzz)
                    try {
                        val dynamicClazz =
                            Class.forName(targetClazz.name, true, combinedLoader)
                        context.scan(dynamicClazz.packageName)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                })
                refresh()
            }

            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lInitialize SpringBoot Application Context Completed...")


            val commandHandler = this.context.getBean(BukkitCommandHandler::class.java)
            val commandAdvices = this.context.getBeansWithAnnotation(
                CommandAdvice::class.java
            )
            commandAdvices.forEach { (t: String?, ob: Any?) ->
                commandHandler.registerAdvices(
                    ob
                )
            }
            val commandControllers = this.context.getBeansWithAnnotation(
                CommandController::class.java
            )
            commandControllers.forEach { (t: String?, ob: Any?) ->
                commandHandler.registerCommands(
                    ob
                )
            }

            val beans: Collection<Listener> = this.context.getBeansOfType(
                Listener::class.java
            ).values
            val eventService = this.context.getBean(EventService::class.java)
            beans.forEach(Consumer { listener: Listener ->
                eventService.registerEvents(
                    listener
                )
            })

            watch.stop()
            Bukkit.getConsoleSender()
                .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Finished. Elapsed Time: " + watch.totalTimeMillis + "ms")
        }, springExecutor).get()
    }

    override fun onDisable() {
        context.close()
    }

}
