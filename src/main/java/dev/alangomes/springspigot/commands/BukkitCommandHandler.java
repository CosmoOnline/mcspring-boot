package dev.alangomes.springspigot.commands;

import dev.alangomes.springspigot.annotation.CommandMapping;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

public class BukkitCommandHandler {
    protected static HashMap<Class<?>, Object> globalInstanceMap = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(BukkitCommandHandler.class);
    private static final HashMap<String, BukkitCommandImpl> mainCMD = new HashMap<>();

    public static void registerCommands(Object cls) {
        try {
            Object cl = cls;
            for (Method mt : cl.getClass().getMethods()) {
                CommandMapping at = mt.getAnnotation(CommandMapping.class);
                if (at != null) {
                    $registerCommands(at, mt, cl);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void $registerCommands(CommandMapping ano, Method mtd, Object cl) {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            if (mainCMD.containsKey(ano.parent())) {
                BukkitCommandImpl b = mainCMD.get(ano.parent());
                b.addCommand(ano.child(), new SubCommandContainer(mtd, ano, cl, ano.subcommand()));
                if (b.getAliases() == null || b.getAliases().size() < ano.aliases().length) {
                    b.setAliases(Arrays.asList(ano.aliases()));
                }
                logger.info("&f&l[&6CosmoAPI Command Initializer&f&l] &a/" + ano.parent() + " " + ano.child() + " &f&lCommand Successfully Initialized");
                return;
            }
            BukkitCommandImpl a = new BukkitCommandImpl(ano, mtd);
            a.addCommand(ano.child(), new SubCommandContainer(mtd, ano, cl, ano.subcommand()));
            commandMap.register(ano.parent(), a);
            mainCMD.put(ano.parent(), a);
            if (a.getAliases() == null || a.getAliases().size() < ano.aliases().length) {
                a.setAliases(Arrays.asList(ano.aliases()));
            }
            logger.info("&f&l[&6CosmoAPI Command Initializer&f&l] &a/" + ano.parent() + " " + ano.child() + " &f&lCommand Successfully Initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
