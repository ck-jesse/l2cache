package com.jd.platform.hotkey.client.core.eventbus;

import com.google.common.eventbus.EventBus;

/**
 * @author wuweifeng wrote on 2020-01-07
 * @version 1.0
 */
@SuppressWarnings("UnstableApiUsage")
public class EventBusCenter {

    private static final EventBus eventBus = new EventBus();

    private EventBusCenter() {

    }

    public static EventBus getInstance() {
        return eventBus;
    }

    public static void register(Object obj) {
        eventBus.register(obj);
    }

    public static void unregister(Object obj) {
        eventBus.unregister(obj);
    }

    public static void post(Object obj) {
        eventBus.post(obj);
    }

}