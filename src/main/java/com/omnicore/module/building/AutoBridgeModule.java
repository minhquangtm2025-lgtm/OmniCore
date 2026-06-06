package com.omnicore.module.building;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import net.minecraft.block.AirBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public class AutoBridgeModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public AutoBridgeModule() {
        super("AutoBridge", "Automatically place blocks under your feet while walking over gaps", ModuleCategory.BUILDING);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        BlockPos below = mc.player.getBlockPos().down();
        if (!mc.world.getBlockState(below).isAir()) return; // block already there

        // Find a block to place in hotbar
        int blockSlot = findBlockSlot();
        if (blockSlot == -1) return;

        mc.player.getInventory().selectedSlot = blockSlot;

        // Look down while bridging
        mc.player.setPitch(80f);

        BlockHitResult hit = new BlockHitResult(
            Vec3d.ofCenter(below.down()), Direction.UP, below.down(), false
        );
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
    }

    private int findBlockSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }
}
