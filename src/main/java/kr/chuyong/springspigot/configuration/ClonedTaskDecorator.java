package kr.chuyong.springspigot.configuration;

import kr.chuyong.springspigot.commands.CommandContext;
import org.springframework.core.task.TaskDecorator;

public class ClonedTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        CommandContext context = CommandContext.getCurrentContext();
        return () -> {
            try {
                CommandContext.setCurrentContext(context);
                task.run();
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                CommandContext.clearContext();
            }
        };
    }
}
