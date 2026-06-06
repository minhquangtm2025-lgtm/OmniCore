package com.omnicore.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class PlayerUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static PlayerEntity getPlayer() {
        return mc.player;
    }

    public static boolean isPlayerValid() {
        return mc.player != null && mc.world != null;
    }

    public static Vec3d getPos() {
        return mc.player != null ? mc.player.getPos() : Vec3d.ZERO;
    }

    public static double distanceTo(Vec3d target) {
        if (mc.player == null) return Double.MAX_VALUE;
        return mc.player.getPos().distanceTo(target);
    }

    public static float getHealth() {
        return mc.player != null ? mc.player.getHealth() : 0;
    }

    public static float getMaxHealth() {
        return mc.player != null ? mc.player.getMaxHealth() : 20;
    }

    public static float getHealthPercent() {
        float max = getMaxHealth();
        return max > 0 ? (getHealth() / max) * 100f : 0;
    }

    public static int getFood() {
        return mc.player != null ? mc.player.getHungerManager().getFoodLevel() : 0;
    }

    public static boolean isOnGround() {
        return mc.player != null && mc.player.isOnGround();
    }

    public static boolean isInWater() {
        return mc.player != null && mc.player.isTouchingWater();
    }

    public static boolean isRiding() {
        return mc.player != null && mc.player.hasVehicle();
    }
}
