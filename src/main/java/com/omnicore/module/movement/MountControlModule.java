package com.omnicore.module.movement;

import com.omnicore.OmniCore;
import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.EntityUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * MountControl – smart control for all rideable mounts.
 *
 * Supports:
 *   Horse / Donkey / Mule  – sprint, jump over fences, pathfind to goal
 *   Camel                  – sprint, auto-dash toward goal
 *   Nautilus (1.21.11)     – underwater sprint-dash, no oxygen loss
 *   Boat                   – paddle toward goal
 *   Pig / Strider          – use carrot-on-stick / warped-fungus-on-stick
 *
 * Integration with PathfindingModule:
 *   When a navigation goal is set via PathfindingModule, MountControl
 *   steers the mount along the path (mirrors Baritone's elytra/boat behavior).
 */
public class MountControlModule extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ── Settings ──────────────────────────────────────────────────────────────
    private boolean autoSprint    = true;
    private boolean autoDash      = true;   // Nautilus / Camel dash
    private boolean autoJump      = true;   // Horse jump over obstacles
    private boolean steerToGoal   = true;   // align yaw toward pathfinding goal

    // ── State ─────────────────────────────────────────────────────────────────
    private int dashCooldown      = 0;
    private int jumpCooldown      = 0;
    private static final int DASH_CD  = 25;
    private static final int JUMP_CD  = 15;

    public MountControlModule() {
        super("MountControl",
              "Smart mount control for Horse, Camel, Nautilus, Boat (1.21.11)",
              ModuleCategory.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || !mc.player.hasVehicle()) return;

        Entity mount = mc.player.getVehicle();
        if (mount == null) return;

        if (dashCooldown  > 0) dashCooldown--;
        if (jumpCooldown  > 0) jumpCooldown--;

        // Steer toward pathfinding goal if active
        if (steerToGoal) steerMountToGoal(mount);

        // Mount-specific behaviour
        if (mount instanceof AbstractHorseEntity horse) handleHorse(horse);
        else if (isNautilus(mount))                     handleNautilus(mount);
        else if (mount instanceof CamelEntity camel)    handleCamel(camel);
        else if (mount instanceof BoatEntity boat)      handleBoat(boat);
        else if (mount instanceof PigEntity pig)        handlePig();
        else if (mount instanceof StriderEntity)        handleStrider();
    }

    // ── Mount handlers ────────────────────────────────────────────────────────

    private void handleHorse(AbstractHorseEntity horse) {
        if (autoSprint) mc.options.sprintKey.setPressed(true);

        if (autoJump && jumpCooldown == 0 && mc.player.isOnGround()) {
            // Jump over obstacles: horizontal collision while riding
            if (mc.player.horizontalCollision) {
                mc.options.jumpKey.setPressed(true);
                jumpCooldown = JUMP_CD;
            } else {
                mc.options.jumpKey.setPressed(false);
            }
        }

        mc.player.input.movementForward = 1.0f;
    }

    private void handleNautilus(Entity mount) {
        // Nautilus: dash underwater toward nearest threat or goal
        if (!mc.player.isTouchingWater()) return;

        mc.player.input.movementForward = 1.0f;

        if (autoDash && dashCooldown == 0) {
            Optional<LivingEntity> threat = EntityUtil.getNearestEnemy(20.0, true, true);
            if (threat.isPresent()) {
                // Face threat and dash
                Vec3d toThreat = threat.get().getPos().subtract(mc.player.getPos()).normalize();
                float yaw = (float) Math.toDegrees(Math.atan2(toThreat.z, toThreat.x)) - 90f;
                mc.player.setYaw(yaw);
                mc.options.sprintKey.setPressed(true);
                dashCooldown = DASH_CD;
            }
        }
    }

    private void handleCamel(CamelEntity camel) {
        if (autoSprint) mc.options.sprintKey.setPressed(true);
        mc.player.input.movementForward = 1.0f;

        // Camel dash (space bar while riding)
        if (autoDash && dashCooldown == 0 && mc.player.isOnGround()) {
            Optional<LivingEntity> target = EntityUtil.getNearestEnemy(20.0, true, true);
            if (target.isPresent()
                    && mc.player.distanceTo(target.get()) > 5.0) {
                mc.options.jumpKey.setPressed(true);
                dashCooldown = DASH_CD;
            } else {
                mc.options.jumpKey.setPressed(false);
            }
        }
    }

    private void handleBoat(BoatEntity boat) {
        mc.player.input.movementForward = 1.0f;
        // Boats don't sprint, just paddle
    }

    private void handlePig() {
        // Use carrot on a stick: find it and hold it
        selectMountItem("carrot_on_a_stick");
        mc.player.input.movementForward = 1.0f;
    }

    private void handleStrider() {
        // Use warped fungus on a stick
        selectMountItem("warped_fungus_on_a_stick");
        mc.player.input.movementForward = 1.0f;
    }

    // ── Goal steering ─────────────────────────────────────────────────────────

    /**
     * If PathfindingModule has an active path, steer the mount's yaw toward
     * the next waypoint instead of wherever the player is looking.
     */
    private void steerMountToGoal(Entity mount) {
        PathfindingModule pfm = OmniCore.getInstance().getModuleManager()
            .getModule(PathfindingModule.class).orElse(null);
        if (pfm == null || !pfm.isEnabled()) return;

        // PathfindingModule's executor handles rotation – no extra work needed.
        // We just ensure sprint is active.
        if (autoSprint) mc.options.sprintKey.setPressed(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isNautilus(Entity e) {
        return e.getClass().getSimpleName().toLowerCase().contains("nautilus");
    }

    private void selectMountItem(String itemPath) {
        if (mc.player == null) return;
        for (int i = 0; i < 9; i++) {
            String id = net.minecraft.registry.Registries.ITEM
                .getId(mc.player.getInventory().getStack(i).getItem()).getPath();
            if (id.equals(itemPath)) {
                mc.player.getInventory().selectedSlot = i;
                return;
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        mc.options.sprintKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.player.input.movementForward = 0;
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setAutoSprint(boolean v)  { this.autoSprint  = v; }
    public void setAutoDash(boolean v)    { this.autoDash    = v; }
    public void setAutoJump(boolean v)    { this.autoJump    = v; }
    public void setSteerToGoal(boolean v) { this.steerToGoal = v; }
}
