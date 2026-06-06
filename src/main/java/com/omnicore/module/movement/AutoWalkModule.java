package com.omnicore.module.movement;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * AutoWalk – continuous forward movement with full environment handling.
 *
 * Handles:
 *   - Sprint on land
 *   - Auto-jump over 1-block obstacles (horizontal collision)
 *   - Swim to surface (hold jump in water)
 *   - Climb ladders (hold forward)
 *   - Sneak at edges to avoid falling (optional)
 *   - Auto-descend: sneak down when standing on ledge
 */
public class AutoWalkModule extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean autoSprint    = true;
    private boolean sneakAtEdges  = false; // sneak near drops > 3 blocks
    private boolean autoSwim      = true;
    private boolean autoClimb     = true;

    private int jumpCooldown = 0;

    public AutoWalkModule() {
        super("AutoWalk", "Walk forward with sprint/swim/ladder/edge-sneak", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (jumpCooldown > 0) jumpCooldown--;

        boolean inWater  = mc.player.isTouchingWater();
        boolean onLadder = mc.player.isClimbing();
        boolean onGround = mc.player.isOnGround();

        // ── Water ────────────────────────────────────────────────────────────
        if (inWater && autoSwim) {
            mc.player.input.movementForward = 1.0f;
            mc.options.sprintKey.setPressed(true);
            mc.options.jumpKey.setPressed(true); // swim up to surface
            return;
        }
        mc.options.jumpKey.setPressed(false);

        // ── Ladder ───────────────────────────────────────────────────────────
        if (onLadder && autoClimb) {
            mc.player.input.movementForward = 1.0f;
            mc.options.sprintKey.setPressed(false);
            return;
        }

        // ── Sprint ───────────────────────────────────────────────────────────
        mc.player.input.movementForward = 1.0f;
        mc.options.sprintKey.setPressed(autoSprint);

        // ── Jump over obstacles ───────────────────────────────────────────────
        if (mc.player.horizontalCollision && onGround && jumpCooldown == 0) {
            mc.player.jump();
            jumpCooldown = 5;
        }

        // ── Edge sneak ────────────────────────────────────────────────────────
        if (sneakAtEdges) {
            mc.options.sneakKey.setPressed(isNearEdge());
        }
    }

    /**
     * Checks if there is a drop > 3 blocks directly ahead.
     */
    private boolean isNearEdge() {
        if (mc.player == null || mc.world == null) return false;
        Vec3d look = mc.player.getRotationVec(1.0f).multiply(1.5);
        BlockPos ahead = BlockPos.ofFloored(mc.player.getPos().add(look));
        // Check if ground drops away more than 3 blocks ahead
        int drop = 0;
        for (int i = 1; i <= 4; i++) {
            if (mc.world.getBlockState(ahead.down(i)).isAir()) drop++;
            else break;
        }
        return drop >= 3;
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        mc.player.input.movementForward  = 0;
        mc.options.sprintKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
    }

    public void setAutoSprint(boolean v)   { this.autoSprint   = v; }
    public void setSneakAtEdges(boolean v) { this.sneakAtEdges = v; }
    public void setAutoSwim(boolean v)     { this.autoSwim     = v; }
    public void setAutoClimb(boolean v)    { this.autoClimb    = v; }
}
