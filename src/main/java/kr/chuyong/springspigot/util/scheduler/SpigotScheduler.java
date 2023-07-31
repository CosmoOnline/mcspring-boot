package kr.chuyong.springspigot.util.scheduler;

import kr.chuyong.springspigot.context.Context;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

public class SpigotScheduler implements TaskScheduler {
    private ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();

    private final SchedulerService scheduler;

    private final Context context;

    public SpigotScheduler(SchedulerService scheduler, Context context) {
        this.scheduler = scheduler;
        this.context = context;

        taskScheduler.setPoolSize(1);
        taskScheduler.initialize();
    }

    private Runnable wrapSync(Runnable task) {
        return new WrappedRunnable(scheduler, task);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        return taskScheduler.schedule(wrapSync(task), trigger);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
        return taskScheduler.schedule(wrapSync(task), startTime);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
        return taskScheduler.scheduleAtFixedRate(wrapSync(task), startTime, period);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
        return taskScheduler.scheduleAtFixedRate(wrapSync(task), period);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
        return taskScheduler.scheduleWithFixedDelay(wrapSync(task), startTime, delay);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
        return taskScheduler.scheduleWithFixedDelay(wrapSync(task), delay);
    }
}
