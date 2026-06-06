package com.omnicore.event;

import com.omnicore.OmniCore;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class EventManager {

    public void registerEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                EventBus.getInstance().post(GameEvents.TickEvent.INSTANCE);
                OmniCore.getInstance().getModuleManager().onTick();
            }
        });
    }
}
