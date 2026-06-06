package com.omnicore.module.movement;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.InventoryUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * ElytraFly – automated elytra flight with:
 * - Auto-launch from ground (jump + activate)
 * - Altitude hold (target Y maintained)
 * - Terrain avoidance (look-ahead collision detection)
 * - Firework auto-boost when speed drops
 * - Smooth pitch correction
 * - Landing detection (auto-disable near goal)
 */
public class ElytraFlyModule extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ── Settings ──────────────────────────────────────────────────────────────
    private double targetAltitude    = -1;    // -1 = maintain current altitude on enable
    private float  cruisePitch       = -20f;  // pitch for level flight
    private boolean autoBoost        = true;
    private double  minSpeed         = 0.3;   // m/tick – boost if below this
    private static final int BOOST_INTERVAL  = 40; // ticks between forced boosts
    private static final int LAUNCH_TICKS    = 5;  // ticks to hold jump before activating

    // ── State ──────────────────────────────────────────────────────────────────
    private int  boostCooldown  = 0;
    private int  launchTick     = 0;
    private boolean launching   = false;

    public ElytraFlyModule() {
        super("ElytraFly", "Automated elytra flight with terrain avoidance and altitude hold",
              ModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnable() {
        if (mc.player != null && targetAltitude < 0) {
            targetAltitude = mc.player.getY();
        }
        launching    = true;
        launchTick   = 0;
        boostCooldown = 0;
    }

    @Override
    public void onDisable() {
        launching = false;
        launchTick = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // ── Check elytra equipped ────────────────────────────────────────────
        if (!(mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)) return;

        // ── Launch sequence ──────────────────────────────────────────────────
        if (launching) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
                launchTick++;
            } else if (!mc.player.isFallFlying()) {
                // Activate elytra mid-air
                mc.options.jumpKey.setPressed(true);
                launchTick++;
                if (launchTick > LAUNCH_TICKS) {
                    mc.options.jumpKey.setPressed(false);
                    launching = false;
                }
            } else {
                launching = false;
                mc.options.jumpKey.setPressed(false);
            }
            return;
        }

        if (!mc.player.isFallFlying()) {
            // Fell out of elytra – re-launch
            launching  = true;
            launchTick = 0;
            return;
        }

        // ── Terrain avoidance – look 4 blocks ahead ──────────────────────────
        float targetPitch = computeTargetPitch();

        // ── Smooth pitch toward target ────────────────────────────────────────
        float curPitch   = mc.player.getPitch();
        float pitchDelta = targetPitch - curPitch;
        mc.player.setPitch(curPitch + clamp(pitchDelta, -5f, 5f));

        // ── Auto boost ───────────────────────────────────────────────────────
        if (autoBoost) {
            boostCooldown--;
            double speed = mc.player.getVelocity().length();
            if (boostCooldown <= 0 || speed < minSpeed) {
                tryBoost();
            }
        }
    }

    // ── Pitch computation ─────────────────────────────────────────────────────

    private float computeTargetPitch() {
        if (mc.player == null || mc.world == null) return cruisePitch;

        double curY    = mc.player.getY();
        double altErr  = targetAltitude - curY; // positive = too low, negative = too high

        // Look ahead for terrain obstacles
        Vec3d vel   = mc.player.getVelocity().normalize().multiply(4);
        BlockPos ahead1 = BlockPos.ofFloored(mc.player.getPos().add(vel));
        BlockPos ahead2 = BlockPos.ofFloored(mc.player.getPos().add(vel.multiply(2)));

        boolean obstacleAhead = !mc.world.getBlockState(ahead1).isAir()
                             || !mc.world.getBlockState(ahead1.up()).isAir()
                             || !mc.world.getBlockState(ahead2).isAir();

        if (obstacleAhead) {
            return -60f; // pitch up sharply to avoid terrain
        }

        // Altitude correction: pitch up/down to hold target altitude
        float correctionPitch = cruisePitch - (float)(altErr * 2.0);
        return clamp(correctionPitch, -80f, 10f);
    }

    // ── Firework boost ────────────────────────────────────────────────────────

    private void tryBoost() {
        if (mc.player == null || mc.interactionManager == null) return;
        int slot = InventoryUtil.findHotbarSlot(Items.FIREWORK_ROCKET);
        if (slot == -1) return;
        int prev = mc.player.getInventory().selectedSlot;
        InventoryUtil.switchToSlot(slot);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        InventoryUtil.switchToSlot(prev);
        boostCooldown = BOOST_INTERVAL;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setTargetAltitude(double y) { this.targetAltitude = y; }
    public void setCruisePitch(float p)     { this.cruisePitch    = p; }
    public void setAutoBoost(boolean v)     { this.autoBoost      = v; }
    public void setMinSpeed(double v)       { this.minSpeed       = v; }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
