package kr.chuyong.springspigot.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.slf4j.MDC;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;

public class BukkitCommandImpl extends BukkitCommand {
    final SubCommandContainer mainContainer = new SubCommandContainer(null, this.getLabel(), 0);
    SuperCommandConfig baseConfig;

    public BukkitCommandImpl(String baseLabel) {
        super(baseLabel);
        this.description = "";
        this.usageMessage = "";
        this.setPermission("");
        this.setAliases(new ArrayList<>());
    }

    public BukkitCommandImpl(String baseLabel, SuperCommandConfig baseConfig) {
        this(baseLabel);
        this.baseConfig = baseConfig;
    }

    public SuperCommandConfig getBaseConfig() {
        return baseConfig;
    }

    public SubCommandContainer getContainer(String[] args) {
        return mainContainer.getContainer(new LinkedList<>(Arrays.asList(args)));
    }

    public SubCommandContainer addCommand(String[] subcommand, CommandConfig ano, Method mtd, Object cl) {
        LinkedList<String> commandList = new LinkedList<>(Arrays.asList(subcommand));
        commandList.removeIf(element -> element.equals(""));
        return mainContainer.addCommand(commandList, ano, mtd, cl);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        SubCommandContainer sc = getContainer(args);
        if (sc == null || !sc.isImplemented()) {
            // System.out.println("UNKNOWN COMMAND");
            return false;
        }

        String[] copiedArray = new String[args.length - sc.getCommandDepth()];
        System.arraycopy(args, sc.getCommandDepth(), copiedArray, 0, copiedArray.length);

        CommandConfig config = sc.getConfig();
        if (!(copiedArray.length >= config.minArgs() && copiedArray.length <= config.maxArgs())) {
            sender.sendMessage(getPrefix(config) + config.usage());
            return false;
        }
        if (checkPermValid(sender, sc.getConfig())) {
            executeMethod(sc, sender, copiedArray);
        }
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        SubCommandContainer sc = getContainer(args);
        if (sc == null) return super.tabComplete(sender, alias, args);
        Collection<String> keys = sc.childCommandKeys();
        return keys.stream().toList();
    }

    private boolean checkPermValid(CommandSender sender, CommandConfig commandConfig) {
        if (opCondition(commandConfig) && !sender.isOp()) {
            sender.sendMessage(getPrefix(commandConfig) + noPermMessage(commandConfig));
            return false;
        }
        if (!consoleCondition(commandConfig) && (!(sender instanceof Player))) {
            sender.sendMessage(getPrefix(commandConfig) + noConsoleMessage(commandConfig));
            return false;
        }
        if (!commandConfig.perm().equals("") && !sender.hasPermission(commandConfig.perm())) {
            sender.sendMessage(getPrefix(commandConfig) + noPermMessage(commandConfig));
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
        try{
            CommandContext.setCurrentContext(new CommandContext(sender));
            sc.getMethod().invoke(sc.getPathClass(), target);
        }catch(Exception e) {
            //Must handled front
            e.printStackTrace();
        } finally {
            CommandContext.clearContext();
        }

      //  ReflectionUtils.invokeMethod(sc.getMethod(), sc.getPathClass(), target);
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

    private String getPrefix(CommandConfig config) {
        return baseConfig != null && !baseConfig.prefix().equals("") ? baseConfig.prefix() : config.prefix();
    }

    private Boolean opCondition(CommandConfig config) {
        return baseConfig != null && !baseConfig.op() || config.op();
    }

    private boolean consoleCondition(CommandConfig config) {
        return baseConfig != null && !baseConfig.console() || config.console();
    }

    private String noPermMessage(CommandConfig config) {
        return baseConfig != null && !baseConfig.noPermMessage().equals("") ? baseConfig.noPermMessage() : config.noPermMessage();
    }

    private String noConsoleMessage(CommandConfig config) {
        return baseConfig != null && !baseConfig.noConsoleMessage().equals("") ? baseConfig.noConsoleMessage() : config.noConsoleMessage();
    }
}
