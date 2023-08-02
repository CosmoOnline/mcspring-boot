package kr.chuyong.springspigot.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;

@Getter
@RequiredArgsConstructor
public class CommandContext {
    public static final ThreadLocal<CommandContext> COMMAND_CONTEXT = new ThreadLocal<>();

    public static CommandContext getCurrentContext() {
        return COMMAND_CONTEXT.get();
    }

    public static void setCurrentContext(CommandContext context) {
        COMMAND_CONTEXT.set(context);
    }

    public static void clearContext() {
        COMMAND_CONTEXT.remove();
    }

    private final CommandSender sender;
}
