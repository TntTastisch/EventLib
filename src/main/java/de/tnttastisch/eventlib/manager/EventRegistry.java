package de.tnttastisch.eventlib.manager;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import de.tnttastisch.eventlib.core.EventSubscriber;

import java.lang.reflect.Method;

/**
 * Central registry for listener registration.
 */
public class EventRegistry {
    private final SimpleEventDispatcher dispatcher = new SimpleEventDispatcher();

    public void registerListener(EventSubscriber listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            Preconditions.checkArgument(!method.isAnnotationPresent(Subscribe.class),
                    "Listener %s is using deprecated @Subscribe annotation! Please update to @SubscribeEvent.", listener);
        }
        dispatcher.register(listener);
    }

    public void unregisterListener(EventSubscriber listener) {
        dispatcher.unregister(listener);
    }

    public SimpleEventDispatcher getDispatcher() {
        return dispatcher;
    }
}
