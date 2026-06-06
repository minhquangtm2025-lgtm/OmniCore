package com.omnicore.mixin;

import com.omnicore.OmniCore;
import com.omnicore.event.EventBus;
import com.omnicore.event.GameEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(boolean tick, CallbackInfo ci) {
        if (OmniCore.getInstance() != null) {
            OmniCore.getInstance().getModuleManager().onRender();
        }
    }
}
