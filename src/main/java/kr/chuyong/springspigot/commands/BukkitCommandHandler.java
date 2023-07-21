package kr.chuyong.springspigot.commands;

import kr.chuyong.springspigot.annotation.CommandMapping;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

public class BukkitCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(BukkitCommandHandler.class);
    private static final HashMap<String, BukkitCommandImpl> mainCMD = new HashMap<>();
    protected static HashMap<Class<?>, Object> globalInstanceMap = new HashMap<>();

    public static void registerCommands(Object cls) {
        try {
            Class<?> commandClazz = AopUtils.getTargetClass(cls);
            CommandMapping baseConfig = null;
            if (commandClazz.isAnnotationPresent(CommandMapping.class)) {
                baseConfig = commandClazz.getAnnotation(CommandMapping.class);
                registerParentCommand(baseConfig);
            }
            for (Method mt : ReflectionUtils.getAllDeclaredMethods(commandClazz)) {
                CommandMapping at = mt.getAnnotation(CommandMapping.class);
                if (at != null) {
                    if (baseConfig == null) {
                        //parent가 없을때
                        registerChildCommands(at.value(), new String[]{at.child()}, CommandConfig.fromAnnotation(at), mt, cls);
                    } else {
                        //class parent가 있을때
                        registerChildCommands(baseConfig.value(), new String[]{baseConfig.child(), at.value(), at.child()}, CommandConfig.fromAnnotation(at), mt, cls);
                    }

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static BukkitCommandImpl registerParentCommand(CommandMapping ano) {
        if (ano.value().equals("") && ano.child().equals(""))
            throw new RuntimeException("Cannot Register non-named class commands");
        if (mainCMD.containsKey(ano.value())) return mainCMD.get(ano.value());
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            BukkitCommandImpl a = new BukkitCommandImpl(ano.value(), SuperCommandConfig.fromAnnotation(ano));
            commandMap.register(ano.value(), a);

            mainCMD.put(ano.value(), a);
            if (a.getAliases().size() < ano.aliases().length) {
                a.setAliases(Arrays.asList(ano.aliases()));
            }
            return a;
        } catch (Exception ex) {
            throw new RuntimeException();
        }
    }

    private static BukkitCommandImpl registerChildCommands(String parentKey, String[] childKey, CommandConfig ano, Method mtd, Object cl) {
        System.out.println("Register parent: " + parentKey + " child: " + Arrays.toString(childKey));
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            BukkitCommandImpl bukkitCommand = mainCMD.get(parentKey);
            if (bukkitCommand == null) {
                bukkitCommand = new BukkitCommandImpl(parentKey);
                commandMap.register(parentKey, bukkitCommand);
                mainCMD.put(parentKey, bukkitCommand);
            }

            SubCommandContainer container = bukkitCommand.addCommand(childKey, ano, mtd, cl);
            Bukkit.getConsoleSender().sendMessage("§f§l[§6SpringSpigot§f§l] §a/" + container.getFullKey() + " §f§lCommand Successfully Initialized");
            return bukkitCommand;

//            if (bukkitCommand.getAliases().size() < ano.aliases().length) {
//                bukkitCommand.setAliases(Arrays.asList(ano.aliases()));
//            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
