package kr.chuyong.springspigot.reactive;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import kr.chuyong.springspigot.context.Context;
import kr.chuyong.springspigot.event.EventUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
class EventEmitter<T extends Event> implements ObservableOnSubscribe<T> {

    @Getter
    Listener listener = new Listener() {
    };

    Class<? extends Event> eventClazz;

    ObserveEvent observeEvent;

    Plugin plugin;

    Context context;


    @Override
    public void subscribe(@NonNull ObservableEmitter<T> emitter) throws Throwable {
        val pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvent(eventClazz, listener, observeEvent.priority(), (l, event) -> {
            if (eventClazz.isAssignableFrom(event.getClass())) {
                T emittedEvent = (T) event;
                context.runWithSender(EventUtil.getSender(emittedEvent), () -> emitter.onNext(emittedEvent));
            }
        }, plugin, observeEvent.ignoreCancelled());
    }
}
