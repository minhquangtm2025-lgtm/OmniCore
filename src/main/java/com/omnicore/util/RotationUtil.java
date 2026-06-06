package com.omnicore.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static float[] getRotationsToEntity(Entity entity) {
        if (mc.player == null) return new float[]{0, 0};
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = entity.getEyePos();
        return getRotationsToPos(eyePos, targetPos);
    }

    public static float[] getRotationsToPos(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
    }

    public static void rotateTo(float yaw, float pitch) {
        if (mc.player == null) return;
        mc.player.setYaw(yaw);
        mc.player.setPitch(MathHelper.clamp(pitch, -90f, 90f));
    }

    public static void smoothRotateTo(float targetYaw, float targetPitch, float speed) {
        if (mc.player == null) return;
        float curYaw = mc.player.getYaw();
        float curPitch = mc.player.getPitch();
        float newYaw = curYaw + MathHelper.clamp(wrapDegrees(targetYaw - curYaw), -speed, speed);
        float newPitch = curPitch + MathHelper.clamp(targetPitch - curPitch, -speed, speed);
        rotateTo(newYaw, newPitch);
    }

    public static float wrapDegrees(float degrees) {
        degrees %= 360f;
        if (degrees >= 180f) degrees -= 360f;
        if (degrees < -180f) degrees += 360f;
        return degrees;
    }

    // Predict where the entity will be after `ticks` ticks
    public static Vec3d predictPosition(Entity entity, int ticks) {
        Vec3d vel = entity.getVelocity();
        return entity.getPos().add(vel.multiply(ticks));
    }
}
