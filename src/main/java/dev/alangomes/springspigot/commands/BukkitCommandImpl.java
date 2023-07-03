package dev.alangomes.springspigot.commands;

import dev.alangomes.springspigot.annotation.CommandMapping;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class BukkitCommandImpl extends BukkitCommand {
    private final HashMap<String, SubCommandContainer> map = new HashMap<String, SubCommandContainer>();
    private SubCommandContainer con = null;

    public BukkitCommandImpl(CommandMapping ano, Method method) {
        super(ano.parent());
        this.description = "";
        this.usageMessage = "";
        this.setPermission("");
        this.setAliases(new ArrayList<>());
    }

    public void addCommand(String subcommand, SubCommandContainer sc) {
        if (!subcommand.equals(""))
            map.put(subcommand, sc);
        else
            con = sc;
    }

    private SubCommandContainer getSubCommand(String subcommand) {
        return map.get(subcommand);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args.length == 0) {
            if (con != null) {
                if (checkPermValid(sender, con)) {
                    executeMethod(con, sender, args);
                }
            }
            return true;
        }
        SubCommandContainer sc = getSubCommand(args[0]);
        if (sc == null) {
            if (con != null) {
                if (checkPermValid(sender, con)) {
                    executeMethod(con, sender, args);
                }
            }
            return false;
        }
        if (!(args.length >= sc.getAnnotation().minArgs() && args.length <= sc.getAnnotation().maxArgs())) {
            sender.sendMessage(sc.getAnnotation().prefix() + sc.getAnnotation().usage());
            return false;
        }
        if (checkPermValid(sender, sc)) {
            executeMethod(sc, sender, args);
        }

        return false;
    }

    private boolean checkPermValid(CommandSender sender, SubCommandContainer sc) {
        if (sc.getAnnotation().op() && !sender.isOp()) {
            sender.sendMessage(sc.getAnnotation().prefix() + sc.getAnnotation().noPerm());
            return false;
        }
        if (!sc.getAnnotation().console() && (!(sender instanceof Player))) {
            sender.sendMessage(sc.getAnnotation().prefix() + sc.getAnnotation().noConsole());
            return false;
        }
        if (!sc.getAnnotation().perm().equals("") && !sender.hasPermission(sc.getAnnotation().perm())) {
            sender.sendMessage(sc.getAnnotation().prefix() + sc.getAnnotation().noPerm());
            return false;
        }
        return true;
    }

    private void executeMethod(SubCommandContainer sc, CommandSender sender, String[] args) {
        UUID uuid = null;
        if (sender instanceof Player)
            uuid = ((Player) sender).getUniqueId();
        final UUID uk = uuid;
        Object[] target = paramBuilder(sc.getMethod(), getParamContainer(sender, args, this.getName()), uuid);
        ReflectionUtils.invokeMethod(sc.getMethod(), sc.getPathClass(), target);
        // sc.getMethod().invoke(sc.getPathClass(), target);
    }

    private HashMap<Class<?>, Object> getParamContainer(CommandSender sender, String[] args, String label) {
        HashMap<Class<?>, Object> map = new HashMap<>();
        map.put(CommandSender.class, sender);
        map.put(String[].class, args);
        map.put(String.class, label);
        if (sender instanceof Player) {
            map.put(Player.class, sender);
        }
        return map;
    }

    private Object[] paramBuilder(Method method, HashMap<Class<?>, Object> paramContainer, UUID player) {
        Object[] arr = new Object[method.getParameterCount()];
        int pos = 0;
        for (Class<?> type : method.getParameterTypes()) {
            Object obj = paramContainer.get(type);
            if (obj == null) {
                obj = BukkitCommandHandler.globalInstanceMap.get(type);
            }
            arr[pos++] = obj;
        }
        paramContainer.clear();
        return arr;
    }
}
