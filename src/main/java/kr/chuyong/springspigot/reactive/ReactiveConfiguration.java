package kr.chuyong.springspigot.reactive;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import jakarta.annotation.PreDestroy;
import kr.chuyong.springspigot.context.Context;
import lombok.val;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Configuration
@ConditionalOnClass(Observable.class)
class ReactiveConfiguration {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Scope(SCOPE_PROTOTYPE)
    @Bean
    public <T extends Event> Observable<T> eventObservable(InjectionPoint injectionPoint, Plugin plugin, Context context) {
        val observeEvent = injectionPoint.getAnnotation(ObserveEvent.class);
        if (observeEvent == null) return null;
        val eventType = ((DependencyDescriptor) injectionPoint).getResolvableType().getGeneric(0);
        val eventEmitter = new EventEmitter<T>((Class<? extends Event>) eventType.getRawClass(), observeEvent, plugin, context);
        return Observable.create(eventEmitter)
                .doOnSubscribe(compositeDisposable::add)
                .doOnDispose(() -> HandlerList.unregisterAll(eventEmitter.getListener()));
    }

    @PreDestroy
    void destroy() {
        compositeDisposable.dispose();
    }

}
