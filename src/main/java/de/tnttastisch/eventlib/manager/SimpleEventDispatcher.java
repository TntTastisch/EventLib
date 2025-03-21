package de.tnttastisch.eventlib.manager;

import de.tnttastisch.eventlib.annotation.SubscribeEvent;
import de.tnttastisch.eventlib.utils.BoundEventHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleEventDispatcher {
    private final Map<Class<?>, Map<Byte, Map<Object, Method[]>>> listenerMethodsByEvent = new HashMap<>();
    private final Map<Class<?>, BoundEventHandler[]> bakedHandlersByEvent = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();
    private final Logger logger = Logger.getLogger(getClass().getName());

    public void post(Object event) {
        BoundEventHandler[] handlers = bakedHandlersByEvent.get(event.getClass());
        if (handlers != null) {
            for (BoundEventHandler handler : handlers) {
                long start = System.nanoTime();
                try {
                    handler.invoke(event);
                } catch (IllegalAccessException | IllegalArgumentException ex) {
                    throw new IllegalStateException("Error invoking method for event: " + event, ex);
                } catch (InvocationTargetException ex) {
                    logger.log(Level.SEVERE, String.format("Error dispatching event %s to listener %s", event, handler.getListener()), ex.getCause());
                }
                long elapsed = System.nanoTime() - start;
                final long threshold = 50_000_000L;
                if (elapsed > threshold) {
                    logger.severe(String.format("Listener %s took %dms to process event %s!", handler.getListener().getClass().getName(), elapsed / 1_000_000, event));
                }
            }
        }
    }

    private Map<Class<?>, Map<Byte, Set<Method>>> findHandlers(Object listener) {
        Map<Class<?>, Map<Byte, Set<Method>>> handlerMethods = new HashMap<>();
        Set<Method> methods = new HashSet<>();
        Collections.addAll(methods, listener.getClass().getMethods());
        Collections.addAll(methods, listener.getClass().getDeclaredMethods());

        for (Method method : methods) {
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation != null) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1) {
                    logger.info(String.format("Method %s in class %s is annotated with @SubscribeEvent but doesn't have exactly one parameter", method, listener.getClass()));
                    continue;
                }
                method.setAccessible(true);
                byte priority = annotation.priorityLevel().getValue();
                if (!handlerMethods.containsKey(params[0])) {
                    handlerMethods.put(params[0], new HashMap<Byte, Set<Method>>());
                }
                Map<Byte, Set<Method>> priorityMap = handlerMethods.get(params[0]);
                if (!priorityMap.containsKey(priority)) {
                    priorityMap.put(priority, new HashSet<Method>());
                }
                priorityMap.get(priority).add(method);
            }
        }
        return handlerMethods;
    }

    public void register(Object listener) {
        Map<Class<?>, Map<Byte, Set<Method>>> handlers = findHandlers(listener);
        lock.lock();
        try {
            for (Map.Entry<Class<?>, Map<Byte, Set<Method>>> entry : handlers.entrySet()) {
                Class<?> eventType = entry.getKey();
                if (!listenerMethodsByEvent.containsKey(eventType)) {
                    listenerMethodsByEvent.put(eventType, new HashMap<Byte, Map<Object, Method[]>>());
                }
                Map<Byte, Map<Object, Method[]>> priorityMap = listenerMethodsByEvent.get(eventType);
                for (Map.Entry<Byte, Set<Method>> priorityEntry : entry.getValue().entrySet()) {
                    if (!priorityMap.containsKey(priorityEntry.getKey())) {
                        priorityMap.put(priorityEntry.getKey(), new HashMap<Object, Method[]>());
                    }
                    Map<Object, Method[]> listenerMap = priorityMap.get(priorityEntry.getKey());
                    listenerMap.put(listener, priorityEntry.getValue().toArray(new Method[0]));
                }
                bakeHandlers(eventType);
            }
        } finally {
            lock.unlock();
        }
    }

    public void unregister(Object listener) {
        Map<Class<?>, Map<Byte, Set<Method>>> handlers = findHandlers(listener);
        lock.lock();
        try {
            for (Map.Entry<Class<?>, Map<Byte, Set<Method>>> entry : handlers.entrySet()) {
                Class<?> eventType = entry.getKey();
                Map<Byte, Map<Object, Method[]>> priorityMap = listenerMethodsByEvent.get(eventType);
                if (priorityMap != null) {
                    for (Byte priority : entry.getValue().keySet()) {
                        Map<Object, Method[]> listenerMap = priorityMap.get(priority);
                        if (listenerMap != null) {
                            listenerMap.remove(listener);
                            if (listenerMap.isEmpty()) {
                                priorityMap.remove(priority);
                            }
                        }
                    }
                    if (priorityMap.isEmpty()) {
                        listenerMethodsByEvent.remove(eventType);
                    }
                }
                bakeHandlers(eventType);
            }
        } finally {
            lock.unlock();
        }
    }

    private void bakeHandlers(Class<?> eventType) {
        Map<Byte, Map<Object, Method[]>> handlersByPriority = listenerMethodsByEvent.get(eventType);
        if (handlersByPriority != null) {
            List<BoundEventHandler> bakedList = new ArrayList<>();
            List<Byte> sorted = new ArrayList<>(handlersByPriority.keySet());
            Collections.sort(sorted);
            for (byte p : sorted) {
                Map<Object, Method[]> listenerMap = handlersByPriority.get(p);
                for (Map.Entry<Object, Method[]> entry : listenerMap.entrySet()) {
                    for (Method m : entry.getValue()) {
                        bakedList.add(new BoundEventHandler(entry.getKey(), m));
                    }
                }
            }
            bakedHandlersByEvent.put(eventType, bakedList.toArray(new BoundEventHandler[0]));
            return;
        }
        bakedHandlersByEvent.remove(eventType);
    }
}
