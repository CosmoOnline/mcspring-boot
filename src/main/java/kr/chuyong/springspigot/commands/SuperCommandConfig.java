package kr.chuyong.springspigot.commands;

import kr.chuyong.springspigot.annotation.CommandMapping;

public record SuperCommandConfig(
        String args,
        String usage,
        String prefix,
        String perm,
        String errorMessage,
        String noPermMessage,
        String noConsoleMessage,
        boolean op,
        boolean console
) {
    public static SuperCommandConfig fromAnnotation(CommandMapping mapping) {
        return new SuperCommandConfig(
                mapping.child(),
                mapping.usage(),
                mapping.prefix(),
                mapping.perm(),
                mapping.error(),
                mapping.noPerm(),
                mapping.noConsole(),
                mapping.op(),
                mapping.console()
        );
    }

}
