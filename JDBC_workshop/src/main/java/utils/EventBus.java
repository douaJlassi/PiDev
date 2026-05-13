package utils;

import java.util.ArrayList;
import java.util.List;

public class EventBus {
    private static EventBus instance;
    private List<Runnable> listeners = new ArrayList<>();

    private EventBus() {}

    public static EventBus getInstance() {
        if (instance == null) instance = new EventBus();
        return instance;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void publish() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}