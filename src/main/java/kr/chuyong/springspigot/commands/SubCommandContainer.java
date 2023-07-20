package kr.chuyong.springspigot.commands;

import kr.chuyong.springspigot.annotation.CommandMapping;
import lombok.Getter;

import java.lang.reflect.Method;

public class SubCommandContainer {
    Method m;
    Object cl;
    CommandMapping ano;
    @Getter
    String[] subList;

    public SubCommandContainer(Method m, CommandMapping ano, Object cl, String[] subList) {
        this.m = m;
        this.ano = ano;
        this.cl = cl;
        this.subList = subList;
    }

    public Method getMethod() {
        return m;
    }

    public CommandMapping getAnnotation() {
        return ano;
    }

    public Object getPathClass() {
        return cl;
    }
}
