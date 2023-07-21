package kr.chuyong.springspigot.commands;

import java.lang.reflect.Method;
import java.util.*;

public class SubCommandContainer {
    private final SubCommandContainer parent;
    private final String currentKey;
    private InvokeWrapper invokeWrapper;
    private final int commandDepth;
    private final HashMap<String, SubCommandContainer> childCommandMap = new HashMap<String, SubCommandContainer>();

    public SubCommandContainer(SubCommandContainer parent, String currentKey, int commandDepth) {
        this.parent = parent;
        this.currentKey = currentKey;
        this.commandDepth = commandDepth;
    }

    public int getCommandDepth() {
        return commandDepth;
    }

    public Collection<String> childCommandKeys() {
        return childCommandMap.keySet();
    }

    public boolean isImplemented() {
        return invokeWrapper != null;
    }

    public SubCommandContainer getContainer(Queue<String> remainingArgs) {
        if(remainingArgs.isEmpty()) {
            return this;
        } else {
            String nextArg = remainingArgs.poll();
            SubCommandContainer nextCommand = childCommandMap.get(nextArg);
            if(nextCommand != null) {
                return nextCommand.getContainer(remainingArgs);
            } else {
                return this;
            }
        }
    }

    // "a b c"
    public SubCommandContainer addCommand(Queue<String> args, CommandConfig ano, Method mtd, Object cl) {
        System.out.println("ADDCOMMAND " +currentKey +  " arr():" +  args.toString());
        if(args.isEmpty()) {
            System.out.println("BINDED " + currentKey +" on depth " + commandDepth);
            invokeWrapper = new InvokeWrapper(mtd, cl, ano);
            return this;
        } else{
            String nextKey = args.poll();
            return childCommandMap
                    .computeIfAbsent(nextKey, (e) -> new SubCommandContainer(this, nextKey,commandDepth + 1))
                    .addCommand(args, ano, mtd, cl);
        }
    }

    public Method getMethod() {
        return invokeWrapper.commandMethod();
    }

    public CommandConfig getConfig() {
        return invokeWrapper.config();
    }

    public Object getPathClass() {
        return invokeWrapper.objectInstance();
    }

    public String mapToTabbedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(currentKey);
        for(SubCommandContainer commandKey : childCommandMap.values()) {
            sb.append("<>".repeat(Math.max(0, commandDepth)));
            sb.append(commandKey.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public String getFullKey() {
        return parent == null ? currentKey : parent.getFullKey() + " " + currentKey;
    }

    @Override
    public String toString() {
        return currentKey+"("+commandDepth+")" + " -> \n" + mapToTabbedString() ;
    }
}
