package com.omnicore.module.combat;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.EntityUtil;
import com.omnicore.util.InventoryUtil;
import com.omnicore.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;

import java.util.Optional;

public class KillAuraModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private double range = 5.0;
    private boolean targetPlayers = true;
    private boolean targetMobs = true;
    private boolean autoSwitch = true;
    private boolean criticalHits = true;

    private int attackCooldown = 0;

    public KillAuraModule() {
        super("KillAura", "Automatically attacks nearby enemies", ModuleCategory.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        Optional<LivingEntity> targetOpt = EntityUtil.getNearestEnemy(range, targetPlayers, targetMobs);
        if (targetOpt.isEmpty()) return;

        LivingEntity target = targetOpt.get();
        double dist = EntityUtil.distanceTo(target);

        // Rotate toward target
        float[] rotations = RotationUtil.getRotationsToEntity(target);
        RotationUtil.smoothRotateTo(rotations[0], rotations[1], 30f);

        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        // Smart weapon switching
        if (autoSwitch) switchBestWeapon(dist);

        // Critical hit: jump while attacking
        if (criticalHits && mc.player.isOnGround() && dist <= 3.0) {
            mc.player.jump();
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        attackCooldown = 4;
    }

    private void switchBestWeapon(double dist) {
        if (dist <= 3.0) {
            // Close range: sword or axe
            int slot = InventoryUtil.findHotbarSlot(SwordItem.class);
            if (slot == -1) slot = InventoryUtil.findHotbarSlot(AxeItem.class);
            if (slot != -1) InventoryUtil.switchToSlot(slot);
        } else if (dist <= 12.0) {
            // Mid range: bow or crossbow
            int slot = InventoryUtil.findHotbarSlot(BowItem.class);
            if (slot == -1) slot = InventoryUtil.findHotbarSlot(CrossbowItem.class);
            if (slot != -1) InventoryUtil.switchToSlot(slot);
        }
    }

    public void setRange(double range) { this.range = range; }
    public void setTargetPlayers(boolean v) { this.targetPlayers = v; }
    public void setTargetMobs(boolean v) { this.targetMobs = v; }
    public void setAutoSwitch(boolean v) { this.autoSwitch = v; }
    public void setCriticalHits(boolean v) { this.criticalHits = v; }
}
