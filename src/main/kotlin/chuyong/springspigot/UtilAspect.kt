package chuyong.springspigot

import chuyong.springspigot.command.BukkitCommandHandler
import chuyong.springspigot.command.CommandContext
import chuyong.springspigot.command.annotation.CommandMapping
import chuyong.springspigot.scheduler.SchedulerService
import lombok.extern.slf4j.Slf4j
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Slf4j
@Aspect
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
class UtilAspect : AsyncUncaughtExceptionHandler {
    @Autowired
    private val schedulerService: SchedulerService? = null

    @Autowired
    private val server: Server? = null

    @Order(0)
    @Around(
        "within(@(@chuyong.springspigot.synchronize.annotation.Synchronize *) *) " +
                "|| execution(@(@chuyong.springspigot.synchronize.annotation.Synchronize *) * *(..)) " +
                "|| @within(chuyong.springspigot.synchronize.annotation.Synchronize)" +
                "|| execution(@chuyong.springspigot.synchronize.annotation.Synchronize * *(..))"
    )
    @Throws(Throwable::class)
    fun synchronizeCall(joinPoint: ProceedingJoinPoint): Any? {
        if (server!!.isPrimaryThread) {
            return joinPoint.proceed()
        }
        schedulerService!!.scheduleSyncDelayedTask({
            try {
                joinPoint.proceed()
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                //  UtilAspect.log.error("Error in synchronous task", throwable)
            }
        }, 0)
        return null
    }

    @Pointcut("@annotation(commandMapping)")
    fun commandMappingPointcut(commandMapping: CommandMapping?) {
    }

    @AfterThrowing(pointcut = "commandMappingPointcut(commandMapping)", throwing = "ex")
    fun handleCommandMappingException(joinPoint: JoinPoint?, commandMapping: CommandMapping?, ex: Exception) {
        handleCommandMappingException(ex)
    }

    override fun handleUncaughtException(ex: Throwable, method: Method, vararg params: Any) {
        //Async로 발생한거긴 한데;; 씹어야해 ㅠㅠ
        ex.printStackTrace()
    }

    fun handleCommandMappingException(ex: Throwable) {
        for ((key, value) in BukkitCommandHandler.exceptionHandlers) {
            if (key.isInstance(ex)) {
                try {
                    val context = CommandContext.currentContext
                    val paramContainer = HashMap<Class<*>, Any>()
                    paramContainer[CommandContext::class.java] = context
                    paramContainer[CommandSender::class.java] = context.sender
                    paramContainer[key] = ex
                    paramContainer[Throwable::class.java] = ex
                    val builtParam = paramBuilder(value.method, paramContainer)
                    value.method.invoke(value.obj, *builtParam)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                } catch (e: InvocationTargetException) {
                    throw RuntimeException(e)
                }
                return
            }
        }
        System.err.println("Uncaught Exception: " + ex.javaClass.simpleName)
        ex.printStackTrace()
    }

    private fun paramBuilder(method: Method, paramContainer: HashMap<Class<*>, Any>): Array<Any?> {
        val arr = arrayOfNulls<Any>(method.parameterCount)
        var pos = 0
        for (type in method.parameterTypes) {
            var obj = paramContainer[type]
            if (obj == null) {
                obj = paramContainer[type.superclass]
                if (obj == null) throw RuntimeException("Unknown Exception Handler parameter type: " + type.name)
            }
            arr[pos++] = obj
        }
        paramContainer.clear()
        return arr
    }
}
