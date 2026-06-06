package com.omnicore.module.combat;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.EntityUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.math.Box;

import java.util.List;

public class AutoShieldModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean shielding = false;

    public AutoShieldModule() {
        super("AutoShield", "Automatically raises shield when under attack", ModuleCategory.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Check if player has a shield in offhand
        if (!(mc.player.getOffHandStack().getItem() instanceof ShieldItem)) return;

        boolean shouldShield = isUnderThreat();

        if (shouldShield && !shielding) {
            mc.options.useKey.setPressed(true);
            shielding = true;
        } else if (!shouldShield && shielding) {
            mc.options.useKey.setPressed(false);
            shielding = false;
        }
    }

    private boolean isUnderThreat() {
        if (mc.world == null || mc.player == null) return false;

        // Incoming arrows
        Box danger = mc.player.getBoundingBox().expand(8.0);
        List<ArrowEntity> arrows = mc.world.getEntitiesByClass(ArrowEntity.class, danger, a -> !a.isOnGround());
        if (!arrows.isEmpty()) return true;

        // Nearby hostile entities about to attack (close range)
        List<LivingEntity> enemies = EntityUtil.getEntitiesInRange(3.5);
        return !enemies.isEmpty();
    }

    @Override
    public void onDisable() {
        if (shielding && mc.player != null) {
            mc.options.useKey.setPressed(false);
            shielding = false;
        }
    }
}
