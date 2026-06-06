package com.omnicore.module.survival;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.InventoryUtil;
import com.omnicore.util.PlayerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.util.Hand;

public class AutoEatModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private int eatThreshold = 16; // eat when food <= this (out of 20)
    private boolean eating = false;
    private int eatTicks = 0;

    public AutoEatModule() {
        super("AutoEat", "Automatically eats best food when hungry", ModuleCategory.SURVIVAL);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        int food = PlayerUtil.getFood();
        if (eating) {
            eatTicks++;
            mc.options.useKey.setPressed(true);
            if (eatTicks >= 32 || food >= 20) { // food items take ~32 ticks
                mc.options.useKey.setPressed(false);
                eating = false;
                eatTicks = 0;
            }
            return;
        }

        if (food <= eatThreshold) {
            int slot = findBestFoodSlot();
            if (slot != -1) {
                InventoryUtil.switchToSlot(slot);
                eating = true;
                eatTicks = 0;
            }
        }
    }

    private int findBestFoodSlot() {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        int bestSlot = -1;
        int bestNutrition = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item instanceof FoodItem || (item.getComponents() != null
                && item.getDefaultStack().contains(net.minecraft.component.DataComponentTypes.FOOD))) {
                var foodComp = item.getDefaultStack().get(net.minecraft.component.DataComponentTypes.FOOD);
                if (foodComp != null && foodComp.nutrition() > bestNutrition) {
                    bestNutrition = foodComp.nutrition();
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    @Override
    public void onDisable() {
        if (eating && mc.player != null) {
            mc.options.useKey.setPressed(false);
            eating = false;
        }
    }

    public void setEatThreshold(int threshold) { this.eatThreshold = threshold; }
}
