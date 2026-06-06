package com.omnicore.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final Map<Class<? extends Event>, List<Consumer<Event>>> listeners = new HashMap<>();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> handler) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>())
            .add((Consumer<Event>) handler);
    }

    public <T extends Event> void post(T event) {
        List<Consumer<Event>> handlers = listeners.get(event.getClass());
        if (handlers != null) {
            for (Consumer<Event> handler : handlers) {
                handler.accept(event);
                if (event.isCancelled()) break;
            }
        }
    }
}
