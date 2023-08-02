package chuyong.springspigot.synchronize

import chuyong.springspigot.UtilAspect
import lombok.RequiredArgsConstructor
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAspectJAutoProxy
@EnableAsync
@RequiredArgsConstructor
class AsyncConfiguration(
    private val utilAspect: UtilAspect,
) : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 7
        executor.maxPoolSize = 42
        executor.queueCapacity = 11
        executor.setTaskDecorator(CommandContextDecorator())
        executor.setThreadNamePrefix("spring-executor-")
        executor.initialize()
        return executor
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return utilAspect
    }
}
