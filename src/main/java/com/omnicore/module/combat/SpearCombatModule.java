package com.omnicore.module.combat;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.EntityUtil;
import com.omnicore.util.InventoryUtil;
import com.omnicore.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;

import java.util.Optional;

/**
 * Handles Spear weapon combat (new in Minecraft 1.21.11 Mounts of Mayhem).
 * Jab: quick left-click – fast, hits multiple targets, applies knockback.
 * Charge: hold right-click then release – velocity-based high damage, can dismount riders.
 */
public class SpearCombatModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Distance thresholds
    private double jabMinDist = 1.5;
    private double jabMaxDist = 4.5;
    private double chargeMinDist = 4.0;
    private double chargeMaxDist = 12.0;
    private int jabCps = 8;

    // State
    private int jabCooldown = 0;
    private boolean isCharging = false;
    private int chargeTicks = 0;
    private static final int CHARGE_TICKS_NEEDED = 15;

    public SpearCombatModule() {
        super("SpearCombat", "Smart Spear Jab/Charge combat for 1.21.11", ModuleCategory.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        // Only activate if holding a spear-like weapon
        // Spear is a new item; we identify it by registry name containing "spear"
        ItemStack held = mc.player.getMainHandStack();
        if (!isSpear(held)) return;

        Optional<LivingEntity> targetOpt = EntityUtil.getNearestEnemy(chargeMaxDist, true, true);
        if (targetOpt.isEmpty()) {
            cancelCharge();
            return;
        }

        LivingEntity target = targetOpt.get();
        double dist = EntityUtil.distanceTo(target);

        // Rotate toward target
        float[] rot = RotationUtil.getRotationsToEntity(target);
        RotationUtil.smoothRotateTo(rot[0], rot[1], 35f);

        boolean mounted = mc.player.hasVehicle();

        // When mounted, prefer Charge for maximum speed-based damage
        if (mounted && dist >= chargeMinDist && dist <= chargeMaxDist) {
            doCharge();
            return;
        }

        // Jab range
        if (dist >= jabMinDist && dist <= jabMaxDist) {
            if (isCharging) cancelCharge();
            doJab(target);
            return;
        }

        // Charge range
        if (dist >= chargeMinDist && dist <= chargeMaxDist) {
            doCharge();
        }
    }

    private void doJab(LivingEntity target) {
        if (jabCooldown > 0) { jabCooldown--; return; }
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        jabCooldown = Math.max(1, 20 / jabCps);
    }

    private void doCharge() {
        if (!isCharging) {
            // Start holding right-click to begin charge
            isCharging = true;
            chargeTicks = 0;
        }
        chargeTicks++;
        mc.options.useKey.setPressed(true);

        if (chargeTicks >= CHARGE_TICKS_NEEDED) {
            // Release to launch the charge attack
            mc.options.useKey.setPressed(false);
            isCharging = false;
            chargeTicks = 0;
        }
    }

    private void cancelCharge() {
        if (isCharging) {
            mc.options.useKey.setPressed(false);
            isCharging = false;
            chargeTicks = 0;
        }
    }

    private boolean isSpear(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();
        return id.contains("spear");
    }

    @Override
    public void onDisable() {
        cancelCharge();
    }
}
