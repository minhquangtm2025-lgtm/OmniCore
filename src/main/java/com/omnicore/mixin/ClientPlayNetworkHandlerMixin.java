package com.omnicore.mixin;

import com.omnicore.event.EventBus;
import com.omnicore.event.GameEvents;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        GameEvents.SendPacketEvent event = new GameEvents.SendPacketEvent(packet);
        EventBus.getInstance().post(event);
        if (event.isCancelled()) ci.cancel();
    }
}
