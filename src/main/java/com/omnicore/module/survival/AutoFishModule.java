package com.omnicore.module.survival;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.InventoryUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.util.Hand;

public class AutoFishModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean isCasting = false;
    private int waitTicks = 0;
    private static final int REEL_IN_DELAY = 5;

    public AutoFishModule() {
        super("AutoFish", "Auto cast, detect bite, and reel in fishing rod", ModuleCategory.SURVIVAL);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        int rodSlot = InventoryUtil.findHotbarSlot(FishingRodItem.class);
        if (rodSlot == -1) return;
        InventoryUtil.switchToSlot(rodSlot);

        FishingBobberEntity bobber = mc.player.fishHook;

        if (!isCasting) {
            // Cast the rod
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            isCasting = true;
            waitTicks = 0;
            return;
        }

        if (bobber == null) {
            isCasting = false;
            return;
        }

        // Check if something is biting (bobber dips below water surface)
        if (bobberBiting(bobber)) {
            if (waitTicks++ >= REEL_IN_DELAY) {
                // Reel in
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                isCasting = false;
                waitTicks = 0;
            }
        } else {
            waitTicks = 0;
        }
    }

    private boolean bobberBiting(FishingBobberEntity bobber) {
        // A bite is detected when the bobber's Y velocity is suddenly negative (dipping)
        return bobber.getVelocity().y < -0.1;
    }

    @Override
    public void onDisable() {
        isCasting = false;
    }
}
