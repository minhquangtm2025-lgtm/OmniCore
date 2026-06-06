package com.omnicore.module.combat;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.EntityUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class AutoDodgeModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private double dodgeRange = 6.0;
    private int dodgeCooldown = 0;
    private static final int DODGE_COOLDOWN_TICKS = 10;

    public AutoDodgeModule() {
        super("AutoDodge", "Automatically dodges projectiles and melee attacks", ModuleCategory.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (dodgeCooldown > 0) { dodgeCooldown--; return; }

        // Dodge incoming arrows/projectiles
        if (dodgeIncomingProjectile()) return;

        // Dodge melee from nearby charging enemies
        dodgeMeleeAttack();
    }

    private boolean dodgeIncomingProjectile() {
        if (mc.world == null || mc.player == null) return false;

        Box dangerZone = mc.player.getBoundingBox().expand(dodgeRange);
        List<ArrowEntity> arrows = mc.world.getEntitiesByClass(ArrowEntity.class, dangerZone, a -> !a.isOnGround());

        for (ArrowEntity arrow : arrows) {
            Vec3d arrowVel = arrow.getVelocity();
            Vec3d arrowPos = arrow.getPos();
            Vec3d playerPos = mc.player.getPos();

            // Check if arrow is heading toward player
            Vec3d toPlayer = playerPos.subtract(arrowPos).normalize();
            double dot = arrowVel.normalize().dotProduct(toPlayer);
            if (dot > 0.85) { // arrow is aimed at us
                performDodge(arrowVel);
                return true;
            }
        }
        return false;
    }

    private void dodgeMeleeAttack() {
        List<LivingEntity> nearby = EntityUtil.getEntitiesInRange(3.0);
        for (LivingEntity entity : nearby) {
            // If entity is sprinting toward us at close range, dodge
            if (entity.isSprinting() && EntityUtil.distanceTo(entity) < 2.5) {
                Vec3d toEnemy = entity.getPos().subtract(mc.player.getPos()).normalize();
                performDodge(toEnemy);
                break;
            }
        }
    }

    private void performDodge(Vec3d threatDirection) {
        if (mc.player == null) return;
        // Strafe perpendicular to threat
        Vec3d right = new Vec3d(-threatDirection.z, 0, threatDirection.x).normalize().multiply(0.3);
        Vec3d currentVel = mc.player.getVelocity();
        mc.player.setVelocity(currentVel.add(right));
        dodgeCooldown = DODGE_COOLDOWN_TICKS;
    }
}
