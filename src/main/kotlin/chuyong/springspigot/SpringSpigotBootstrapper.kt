package chuyong.springspigot

import chuyong.springspigot.command.BukkitCommandHandler
import chuyong.springspigot.command.annotation.CommandAdvice
import chuyong.springspigot.command.annotation.CommandController
import chuyong.springspigot.config.ConfigurationPropertySource
import chuyong.springspigot.util.CompoundClassLoader
import chuyong.springspigot.util.YamlPropertiesFactory
import lombok.SneakyThrows
import org.bukkit.Bukkit
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
import java.util.function.Consumer

class SpringSpigotBootstrapper : JavaPlugin() {
    lateinit var masterClassLoader: ClassLoader
    private val logger = LoggerFactory.getLogger(SpringSpigotBootstrapper::class.java)
    lateinit var context: ConfigurableApplicationContext

    init {
        val parentClassLoader = Thread.currentThread().contextClassLoader
        val mainClassLoader = classLoader
        val compoundLoader = CompoundClassLoader(mainClassLoader, parentClassLoader)
        Thread.currentThread().contextClassLoader = compoundLoader
        masterClassLoader = compoundLoader
    }

    fun createClassLoader(parent: ClassLoader?, url: URL): ClassLoader {
        return URLClassLoader(arrayOf(url), parent)
    }

    @SneakyThrows
    override fun onEnable() {
        loadSpringSpigot()
    }

    @SneakyThrows
    fun loadSpringSpigot() {
        val watch = StopWatch()
        watch.start()
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Initiated...")
        val classLoaders = ArrayList<ClassLoader>()
        classLoaders.add(Thread.currentThread().contextClassLoader)
        classLoaders.add(classLoader)
        val pluginClasses = Arrays.stream(Bukkit.getPluginManager().plugins)
            .filter { plugin: Plugin ->
                plugin is JavaPlugin && plugin.javaClass.isAnnotationPresent(
                    EnableSpringSpigotSupport::class.java
                )
            }
            .toList()
        var combinedLoader = CompoundClassLoader(classLoaders)
        Thread.currentThread().contextClassLoader = combinedLoader
        val javaClassLoader = Class.forName("org.bukkit.plugin.java.PluginClassLoader")
        val f = javaClassLoader.getDeclaredField("file")
        f.isAccessible = true
        logger.info("Registering SpringSpigot Supported Plugins...")
        pluginClasses.forEach(Consumer { plugin: Plugin ->
            try {
                val file = f[plugin.javaClass.classLoader] as File
                logger.info("Disabling plugin " + plugin.name + " To load from SpringSpigot..")
                Bukkit.getPluginManager().disablePlugin(plugin)
                val newLoader = createClassLoader(masterClassLoader, file.toURI().toURL())
                classLoaders.add(newLoader)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        })
        logger.info("Registering SpringSpigot Supported Plugins Completed")
        combinedLoader = CompoundClassLoader(classLoaders)
        val ctx = AnnotationConfigApplicationContext()
        ctx.setResourceLoader(DefaultResourceLoader(combinedLoader))
        ctx.classLoader = combinedLoader
        ctx.environment.propertySources.addLast(
            PropertiesPropertySource(
                "main-yaml",
                YamlPropertiesFactory.loadYamlIntoProperties(FileSystemResource("application.yml"))!!
            )
        )
        ctx.environment.propertySources.forEach(Consumer { ptx: PropertySource<*> ->
            println(
                ptx.javaClass.name + " with " + ptx.getProperty("spring.datasource.url")
            )
        })
        context = ctx
        val propertySources: MutablePropertySources = context.environment.propertySources
        propertySources.addLast(ConfigurationPropertySource(config))

        val props = Properties()
        props["spigot.plugin"] = name
        propertySources.addLast(PropertiesPropertySource("spring-bukkit", props))
        ctx.register(SpringSpigotApplication::class.java)

        val finalCombinedLoader1 = combinedLoader
        pluginClasses.forEach(Consumer { pluginzz: Plugin ->
            val targetClazz = AopUtils.getTargetClass(pluginzz)
            try {
                val dynamicClazz =
                    Class.forName(targetClazz.name, true, finalCombinedLoader1)
                ctx.scan(dynamicClazz.packageName)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        })
        ctx.refresh()


        val commandAdvices = context.getBeansWithAnnotation(
            CommandAdvice::class.java
        )
        commandAdvices.forEach { (t: String?, ob: Any?) ->
            BukkitCommandHandler.registerAdvices(
                ob
            )
        }
        val commandControllers = context.getBeansWithAnnotation(
            CommandController::class.java
        )
        commandControllers.forEach { (t: String?, ob: Any?) ->
            BukkitCommandHandler.registerCommands(
                ob
            )
        }
        watch.stop()
        Bukkit.getConsoleSender()
            .sendMessage("§f§l[§6SpringSpigot§f§l] §f§lSpringSpigot Initialization Progress Finished. Elapsed Time: " + watch.totalTimeMillis + "ms")
    }

    override fun onDisable() {
        context.close()
    }

}
