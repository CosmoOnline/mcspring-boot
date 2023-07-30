package kr.chuyong.springspigot.external;

import kotlin.reflect.KClass;
import kr.hqservice.framework.bukkit.core.HQBukkitPlugin;
import kr.hqservice.framework.global.core.component.HQComponent;
import kr.hqservice.framework.global.core.component.registry.ComponentRegistry;
import org.bukkit.Bukkit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component("hq-provider")
@ConditionalOnBean(KoinDependencyProvider.class)
@ConditionalOnClass(ComponentRegistry.class)
public class HQFrameworkDependencyProvider implements ExternalDependencyProvider {
    private final KoinDependencyProvider koinProvider;

    public HQFrameworkDependencyProvider(KoinDependencyProvider koinProvider) {
        this.koinProvider = koinProvider;
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return getNamed(clazz, "HQFramework");
    }

    @Override
    public <T> T getNamed(Class<T> clazz, String qualifier) {
        return (T) getRegistryFromPlugin(qualifier).getComponent((KClass<? extends HQComponent>) kotlin.jvm.JvmClassMappingKt.getKotlinClass(clazz));
    }

    private ComponentRegistry getRegistryFromPlugin(String pluginName) {
        HQBukkitPlugin pluginzz = (HQBukkitPlugin) Bukkit.getPluginManager().getPlugin(pluginName);
        try {
            Method m = HQBukkitPlugin.class.getDeclaredMethod("getComponentRegistry");
            m.setAccessible(true);
            return (ComponentRegistry) m.invoke(pluginzz);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }
}
