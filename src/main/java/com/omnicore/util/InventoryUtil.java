package com.omnicore.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.item.Item;

public class InventoryUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static PlayerInventory getInventory() {
        return mc.player != null ? mc.player.getInventory() : null;
    }

    public static ItemStack getHeldItem() {
        return mc.player != null ? mc.player.getMainHandStack() : ItemStack.EMPTY;
    }

    public static boolean isHolding(Class<? extends Item> itemClass) {
        ItemStack held = getHeldItem();
        return !held.isEmpty() && itemClass.isInstance(held.getItem());
    }

    // Finds slot (0-8 hotbar) of the first item matching class, returns -1 if not found
    public static int findHotbarSlot(Class<? extends Item> itemClass) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) return i;
        }
        return -1;
    }

    public static int findHotbarSlot(Item item) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    public static void switchToSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8) return;
        mc.player.getInventory().selectedSlot = slot;
    }

    public static boolean hasItemInInventory(Item item) {
        if (mc.player == null) return false;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(item)) return true;
        }
        return false;
    }

    public static int countItem(Item item) {
        if (mc.player == null) return 0;
        PlayerInventory inv = mc.player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) count += stack.getCount();
        }
        return count;
    }
}
