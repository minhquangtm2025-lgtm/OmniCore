package com.omnicore.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EntityUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static List<LivingEntity> getEntitiesInRange(double range) {
        if (mc.player == null || mc.world == null) return List.of();
        Box box = mc.player.getBoundingBox().expand(range);
        return mc.world.getEntitiesByClass(LivingEntity.class, box, e ->
            e != mc.player && !e.isDead() && e.isAlive()
        );
    }

    public static Optional<LivingEntity> getNearestEnemy(double range, boolean targetPlayers, boolean targetMobs) {
        return getEntitiesInRange(range).stream()
            .filter(e -> {
                if (e instanceof PlayerEntity) return targetPlayers;
                if (e instanceof HostileEntity) return targetMobs;
                return false;
            })
            .min(Comparator.comparingDouble(e ->
                mc.player.getPos().squaredDistanceTo(e.getPos())
            ));
    }

    public static boolean isHostile(Entity entity) {
        return entity instanceof HostileEntity;
    }

    public static boolean isPlayer(Entity entity) {
        return entity instanceof PlayerEntity && entity != mc.player;
    }

    // Undead mobs are healed by Harming potions and hurt by Healing potions
    public static boolean isUndead(LivingEntity entity) {
        return entity instanceof ZombieEntity
            || entity instanceof SkeletonEntity
            || entity instanceof ZombieHorseEntity
            || entity instanceof WitherSkeletonEntity;
    }

    public static double distanceTo(Entity entity) {
        if (mc.player == null) return Double.MAX_VALUE;
        return mc.player.distanceTo(entity);
    }

    public static boolean canSee(Entity entity) {
        if (mc.player == null || mc.world == null) return false;
        return mc.player.canSee(entity);
    }
}
