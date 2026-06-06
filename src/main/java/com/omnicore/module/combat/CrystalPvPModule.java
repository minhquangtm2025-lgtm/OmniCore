package com.omnicore.module.combat;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.EntityUtil;
import com.omnicore.util.InventoryUtil;
import com.omnicore.util.RotationUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.List;
import java.util.Optional;

public class CrystalPvPModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private double maxPlaceDistance = 4.0;
    private boolean selfProtection = true;
    private int actionCooldown = 0;

    public CrystalPvPModule() {
        super("CrystalPvP", "Auto place and explode End Crystals for advanced PvP", ModuleCategory.COMBAT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (actionCooldown > 0) { actionCooldown--; return; }

        Optional<LivingEntity> targetOpt = EntityUtil.getNearestEnemy(maxPlaceDistance + 2, true, false);
        if (targetOpt.isEmpty()) return;

        LivingEntity target = targetOpt.get();

        // 1. Try to explode any existing crystal near the target
        if (explodeCrystalNear(target)) { actionCooldown = 3; return; }

        // 2. Try to place a crystal near the target
        if (placeCrystalNear(target)) { actionCooldown = 5; }
    }

    private boolean explodeCrystalNear(LivingEntity target) {
        if (mc.world == null) return false;
        List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(
            EndCrystalEntity.class,
            target.getBoundingBox().expand(3.0),
            c -> true
        );
        for (EndCrystalEntity crystal : crystals) {
            if (selfProtection && mc.player.distanceTo(crystal) < 5.0) continue;
            float[] rot = RotationUtil.getRotationsToEntity(crystal);
            RotationUtil.rotateTo(rot[0], rot[1]);
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private boolean placeCrystalNear(LivingEntity target) {
        if (mc.player == null || mc.world == null) return false;
        int crystalSlot = InventoryUtil.findHotbarSlot(Items.END_CRYSTAL);
        if (crystalSlot == -1) return false;

        BlockPos targetBlock = BlockPos.ofFloored(target.getPos());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos placePos = targetBlock.add(dx, 0, dz);
                if (!mc.world.getBlockState(placePos).isOf(Blocks.OBSIDIAN) &&
                    !mc.world.getBlockState(placePos).isOf(Blocks.BEDROCK)) continue;
                if (!mc.world.getBlockState(placePos.up()).isAir()) continue;
                if (mc.player.getPos().distanceTo(Vec3d.ofCenter(placePos)) > maxPlaceDistance) continue;

                InventoryUtil.switchToSlot(crystalSlot);
                BlockHitResult hitResult = new BlockHitResult(
                    Vec3d.ofCenter(placePos), Direction.UP, placePos, false
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                return true;
            }
        }
        return false;
    }
}
