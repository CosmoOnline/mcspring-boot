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
        HQBukkitPlugin pluginzz = (HQBukkitPlugin) Bukkit.getPluginManager().getPlugin("HQFramework");
        try {
            Method m = HQBukkitPlugin.class.getDeclaredMethod("getComponentRegistry");
            m.setAccessible(true);
            ComponentRegistry registry = (ComponentRegistry) m.invoke(pluginzz);
            KClass<? extends HQComponent> kotlinClazz = (KClass<? extends HQComponent>) kotlin.jvm.JvmClassMappingKt.getKotlinClass(clazz);
            return (T) registry.getComponent(kotlinClazz);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public <T> T getNamed(Class<T> clazz, String qualifier) {
        return get(clazz);
    }
}
