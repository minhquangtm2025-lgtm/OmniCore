package com.omnicore.module.survival;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.util.InventoryUtil;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.*;
import net.minecraft.util.math.*;

import java.util.*;

public class AutoMineModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum MineMode { VEIN, TUNNEL }

    private MineMode mode = MineMode.VEIN;
    private Class<? extends Block> targetBlock = DiamondOreBlock.class;
    private int scanRadius = 8;
    private int actionCooldown = 0;
    private final Deque<BlockPos> mineQueue = new ArrayDeque<>();

    public AutoMineModule() {
        super("AutoMine", "Auto mine ores (vein) or dig tunnels", ModuleCategory.SURVIVAL);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (actionCooldown > 0) { actionCooldown--; return; }

        // Auto-select best tool
        autoSelectTool();

        if (mode == MineMode.VEIN) tickVein();
        else tickTunnel();
    }

    private void tickVein() {
        if (mineQueue.isEmpty()) scanForOres();
        if (mineQueue.isEmpty()) return;

        BlockPos target = mineQueue.peekFirst();
        if (mc.world.getBlockState(target).isAir()) {
            mineQueue.pollFirst();
            return;
        }
        if (mc.player.getPos().distanceTo(Vec3d.ofCenter(target)) > 5) return;

        Direction face = getFaceToward(target);
        boolean breaking = mc.interactionManager.updateBlockBreakingProgress(target, face);
        if (!breaking) {
            mc.interactionManager.attackBlock(target, face);
        }
        actionCooldown = 1;
    }

    private void tickTunnel() {
        // Mine 1x2 tunnel in the direction player is facing
        BlockPos front = mc.player.getBlockPos().offset(mc.player.getHorizontalFacing());
        BlockPos frontUp = front.up();
        for (BlockPos pos : new BlockPos[]{front, frontUp}) {
            if (!mc.world.getBlockState(pos).isAir()) {
                mc.interactionManager.attackBlock(pos, mc.player.getHorizontalFacing().getOpposite());
                actionCooldown = 2;
                return;
            }
        }
        mc.player.input.movementForward = 1f;
    }

    private void scanForOres() {
        if (mc.world == null || mc.player == null) return;
        BlockPos center = mc.player.getBlockPos();
        for (int x = -scanRadius; x <= scanRadius; x++)
            for (int y = -scanRadius; y <= scanRadius; y++)
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (targetBlock.isInstance(mc.world.getBlockState(pos).getBlock())) {
                        mineQueue.add(pos);
                    }
                }
    }

    private void autoSelectTool() {
        int pickSlot = InventoryUtil.findHotbarSlot(PickaxeItem.class);
        if (pickSlot != -1) InventoryUtil.switchToSlot(pickSlot);
    }

    private Direction getFaceToward(BlockPos target) {
        Vec3d diff = Vec3d.ofCenter(target).subtract(mc.player.getEyePos());
        double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
        if (ay > ax && ay > az) return diff.y > 0 ? Direction.DOWN : Direction.UP;
        if (ax > az) return diff.x > 0 ? Direction.WEST : Direction.EAST;
        return diff.z > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    public void setMode(MineMode mode) { this.mode = mode; mineQueue.clear(); }
    public void setTargetBlock(Class<? extends Block> blockClass) { this.targetBlock = blockClass; mineQueue.clear(); }
    public void setScanRadius(int r) { this.scanRadius = r; }
}
