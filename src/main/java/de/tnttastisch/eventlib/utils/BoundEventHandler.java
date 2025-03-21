package de.tnttastisch.eventlib.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps an event handler method and its associated listener instance.
 */
public class BoundEventHandler {
    private final Object listener;
    private final Method method;

    public BoundEventHandler(Object listener, Method method) {
        this.listener = listener;
        this.method = method;
    }

    public void invoke(Object event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        method.setAccessible(true);
        method.invoke(listener, event);
    }

    public Object getListener() {
        return listener;
    }

    public Method getMethod() {
        return method;
    }
}
