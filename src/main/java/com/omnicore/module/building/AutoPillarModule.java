package com.omnicore.module.building;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

public class AutoPillarModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private int targetHeight = 10;
    private int startY = 0;

    public AutoPillarModule() {
        super("AutoPillar", "Automatically pillar up by placing blocks under feet", ModuleCategory.BUILDING);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) startY = mc.player.getBlockY();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        int climbed = mc.player.getBlockY() - startY;
        if (climbed >= targetHeight) { setEnabled(false); return; }

        int blockSlot = findBlockSlot();
        if (blockSlot == -1) return;

        mc.player.getInventory().selectedSlot = blockSlot;
        mc.player.jump();

        BlockPos below = mc.player.getBlockPos().down();
        if (mc.world.getBlockState(below).isAir()) {
            BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(below.down()), Direction.UP, below.down(), false
            );
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        }
    }

    private int findBlockSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    public void setTargetHeight(int h) { this.targetHeight = h; }
}
