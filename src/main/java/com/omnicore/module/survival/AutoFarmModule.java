package com.omnicore.module.survival;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

public class AutoFarmModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private int scanRadius = 5;
    private int actionCooldown = 0;

    public AutoFarmModule() {
        super("AutoFarm", "Auto harvest and replant crops", ModuleCategory.SURVIVAL);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (actionCooldown > 0) { actionCooldown--; return; }

        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> maturedCrops = findMaturedCrops(playerPos);
        if (maturedCrops.isEmpty()) return;

        BlockPos crop = maturedCrops.get(0);
        // Harvest
        mc.interactionManager.attackBlock(crop, Direction.UP);
        actionCooldown = 5;

        // Replant after harvesting
        replant(crop);
    }

    private List<BlockPos> findMaturedCrops(BlockPos center) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int z = -scanRadius; z <= scanRadius; z++) {
                BlockPos pos = center.add(x, 0, z);
                BlockState state = mc.world.getBlockState(pos);
                if (isMatureCrop(state)) result.add(pos);
            }
        }
        return result;
    }

    private boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return state.get(crop.getAgeProperty()) >= crop.getMaxAge();
        }
        return false;
    }

    private void replant(BlockPos pos) {
        if (mc.player == null || mc.world == null) return;
        // Find seed in hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AliasedBlockItem) {
                mc.player.getInventory().selectedSlot = i;
                BlockHitResult hit = new BlockHitResult(
                    Vec3d.ofCenter(pos.down()), Direction.UP, pos.down(), false
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                return;
            }
        }
    }

    public void setScanRadius(int r) { this.scanRadius = r; }
}
