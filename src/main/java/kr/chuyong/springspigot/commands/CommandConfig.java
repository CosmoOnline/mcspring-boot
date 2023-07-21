package kr.chuyong.springspigot.commands;

import kr.chuyong.springspigot.annotation.CommandMapping;

public record CommandConfig(
        String usage,
        String prefix,
        String perm,
        String errorMessage,
        String noPermMessage,
        String noConsoleMessage,
        int minArgs,
        int maxArgs,
        boolean op,
        boolean console
){
    public static CommandConfig fromAnnotation(CommandMapping mapping) {
        return new CommandConfig(
                mapping.usage(),
                mapping.prefix(),
                mapping.perm(),
                mapping.error(),
                mapping.noPerm(),
                mapping.noConsole(),
                mapping.minArgs(),
                mapping.maxArgs(),
                mapping.op(),
                mapping.console()
        );
    }

}
