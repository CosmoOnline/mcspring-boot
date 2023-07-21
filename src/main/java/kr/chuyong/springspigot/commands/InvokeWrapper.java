package kr.chuyong.springspigot.commands;

import java.lang.reflect.Method;

public record InvokeWrapper(
        Method commandMethod,
        Object objectInstance,
        CommandConfig config
) {
}
