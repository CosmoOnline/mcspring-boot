package kr.chuyong.springspigot.external;

import kotlin.reflect.KClass;
import org.koin.core.KoinApplication;
import org.koin.core.context.GlobalContext;
import org.koin.core.qualifier.StringQualifier;
import org.koin.core.scope.Scope;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

@Component("koin-provider")
@ConditionalOnClass(KoinApplication.class)
public class KoinDependencyProvider implements ExternalDependencyProvider {
    private Scope getScope() {
        KoinApplication koinApplication = GlobalContext.INSTANCE.getKoinApplicationOrNull();
        if (koinApplication != null) {
            Scope scope = koinApplication.getKoin().getScopeRegistry().getRootScope();
            return scope;
        }
        return null;
    }

    @Override
    public <T> T get(Class<T> clazz) {
        Scope mainScope = getScope();
        if (mainScope == null) throw new RuntimeException("Koin Not Initialized");
        KClass<T> kotlinClazz = kotlin.jvm.JvmClassMappingKt.getKotlinClass(clazz);
        return mainScope.get(kotlinClazz, null, null);
    }

    @Override
    public <T> T getNamed(Class<T> clazz, String qualifier) {
        Scope mainScope = getScope();
        if (mainScope == null) throw new RuntimeException("Koin Not Initialized");
        KClass<T> kotlinClazz = kotlin.jvm.JvmClassMappingKt.getKotlinClass(clazz);
        return mainScope.get(kotlinClazz, new StringQualifier(qualifier), null);
    }
}
