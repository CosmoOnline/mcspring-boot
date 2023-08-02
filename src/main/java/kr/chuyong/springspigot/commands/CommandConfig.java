package kr.chuyong.springspigot.commands;

import kr.chuyong.springspigot.annotation.CommandMapping;

public record CommandConfig(
        String usage,
        String prefix,
        String perm,
        String errorMessage,
        String noPermMessage,
        String noConsoleMessage,
        String[] suggestion,
        int minArgs,
        int maxArgs,
        boolean op,
        boolean console,
        boolean defaultSuggestion
) {
    public static CommandConfig fromAnnotation(CommandMapping mapping) {
        return new CommandConfig(
                mapping.usage(),
                mapping.prefix(),
                mapping.perm(),
                mapping.error(),
                mapping.noPerm(),
                mapping.noConsole(),
                mapping.suggestion(),
                mapping.minArgs(),
                mapping.maxArgs(),
                mapping.op(),
                mapping.console(),
                mapping.defaultSuggestion()
        );
    }

}
