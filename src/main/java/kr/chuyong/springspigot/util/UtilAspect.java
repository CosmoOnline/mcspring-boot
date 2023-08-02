package kr.chuyong.springspigot.util;

import kr.chuyong.springspigot.annotation.CommandMapping;
import kr.chuyong.springspigot.commands.BukkitCommandHandler;
import kr.chuyong.springspigot.commands.CommandContext;
import kr.chuyong.springspigot.commands.InvokeWrapper;
import kr.chuyong.springspigot.util.scheduler.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

@Slf4j
@Aspect
@Component
@Scope(SCOPE_SINGLETON)
public
class UtilAspect implements AsyncUncaughtExceptionHandler {

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private Server server;

    @Order(0)
    @Around("within(@(@kr.chuyong.springspigot.util.Synchronize *) *) " +
            "|| execution(@(@kr.chuyong.springspigot.util.Synchronize *) * *(..)) " +
            "|| @within(kr.chuyong.springspigot.util.Synchronize)" +
            "|| execution(@kr.chuyong.springspigot.util.Synchronize * *(..))")
    public Object synchronizeCall(ProceedingJoinPoint joinPoint) throws Throwable {
        if (server.isPrimaryThread()) {
            return joinPoint.proceed();
        }
        schedulerService.scheduleSyncDelayedTask(() -> {
            try {
                joinPoint.proceed();
            } catch (Throwable throwable) {
                log.error("Error in synchronous task", throwable);
            }
        }, 0);
        return null;
    }

    @Pointcut("@annotation(commandMapping)")
    public void commandMappingPointcut(CommandMapping commandMapping) {
    }

    @AfterThrowing(pointcut = "commandMappingPointcut(commandMapping)", throwing = "ex")
    public void handleCommandMappingException(JoinPoint joinPoint, CommandMapping commandMapping, Exception ex) {
        handleCommandMappingException(ex);
    }

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        //Async로 발생한거긴 한데;; 씹어야해 ㅠㅠ

        ex.printStackTrace();
    }

    public void handleCommandMappingException(Throwable ex) {
        for (Map.Entry<Class<? extends Throwable>, InvokeWrapper> entry : BukkitCommandHandler.exceptionHandlers.entrySet()) {
            if(entry.getKey().isInstance(ex)) {
                try {
                    CommandContext context = CommandContext.getCurrentContext();

                    HashMap<Class<?>, Object> paramContainer = new HashMap<>();
                    paramContainer.put(CommandContext.class, context);
                    paramContainer.put(CommandSender.class, context.getSender());
                    paramContainer.put(entry.getKey(), ex);
                    paramContainer.put(Throwable.class, ex);
                    Object[] builtParam = paramBuilder(entry.getValue().commandMethod(), paramContainer);

                    entry.getValue().commandMethod().invoke(entry.getValue().objectInstance(), builtParam);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
        System.err.println("Uncaught Exception: " + ex.getClass().getSimpleName());
        ex.printStackTrace();
    }

    private Object[] paramBuilder(Method method, HashMap<Class<?>, Object> paramContainer) {
        Object[] arr = new Object[method.getParameterCount()];
        int pos = 0;
        for (Class<?> type : method.getParameterTypes()) {
            Object obj = paramContainer.get(type);
            if (obj == null) {
                obj = paramContainer.get(type.getSuperclass());
                if(obj == null)
                    throw new RuntimeException("Unknown Exception Handler parameter type: " + type.getName());
            }
            arr[pos++] = obj;
        }
        paramContainer.clear();
        return arr;
    }
}
