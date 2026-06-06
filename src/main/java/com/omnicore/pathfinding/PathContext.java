package com.omnicore.pathfinding;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Snapshot of world state and player capabilities used during A* calculation.
 * Equivalent to Baritone's CalculationContext – keeps pathfinding thread-safe
 * by capturing all needed data before the async thread starts.
 */
public class PathContext {

    public final World   world;
    public final boolean canSprint;
    public final boolean canSwim;
    public final boolean hasPickaxe;
    public final boolean hasShovel;
    public final boolean hasElytra;
    public final boolean allowDig;
    public final boolean allowParkour;
    public final int     maxFallHeight; // safe fall in blocks

    public PathContext(boolean allowDig, boolean allowParkour) {
        MinecraftClient mc = MinecraftClient.getInstance();
        this.world        = mc.world;
        this.allowDig     = allowDig;
        this.allowParkour = allowParkour;
        this.maxFallHeight = 3;

        if (mc.player != null) {
            this.canSprint   = true;
            this.canSwim     = true;
            this.hasElytra   = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem;
            boolean pick = false, sho = false;
            for (int i = 0; i < 9; i++) {
                Item item = mc.player.getInventory().getStack(i).getItem();
                if (item instanceof PickaxeItem) pick = true;
                if (item instanceof ShovelItem)  sho  = true;
            }
            this.hasPickaxe = pick;
            this.hasShovel  = sho;
        } else {
            this.canSprint = this.canSwim = this.hasPickaxe
                           = this.hasShovel = this.hasElytra = false;
        }
    }

    // ── Block queries ────────────────────────────────────────────────────────

    /** True if the block is open for the player body to occupy. */
    public boolean isPassable(BlockPos pos) {
        if (world == null) return true;
        BlockState s = world.getBlockState(pos);
        if (s.isAir()) return true;
        Block b = s.getBlock();
        return b instanceof TallPlantBlock
            || b instanceof ShortPlantBlock
            || b instanceof FlowerBlock
            || b instanceof TorchBlock
            || b instanceof WallTorchBlock
            || b instanceof CarpetBlock
            || b instanceof SnowBlock && s.get(SnowBlock.LAYERS) < 3
            || isWater(pos);
    }

    /** True if block can be stood on (solid top, passable above ×2, not dangerous). */
    public boolean isWalkable(BlockPos pos) {
        if (world == null) return false;
        return !isPassable(pos)
            && isPassable(pos.up())
            && isPassable(pos.up(2))
            && !isDangerous(pos)
            && pos.getY() >= world.getBottomY();
    }

    /** Breakable by player tools (respects pickaxe/shovel availability). */
    public boolean isBreakable(BlockPos pos) {
        if (!allowDig || world == null) return false;
        BlockState s = world.getBlockState(pos);
        if (s.isAir() || isPassable(pos)) return false;
        float hard = s.getHardness(world, pos);
        if (hard < 0) return false; // unbreakable (bedrock, end portal)
        Block b = s.getBlock();
        if (b instanceof OreBlock || b instanceof StoneBlock
            || b instanceof GraniteBlock || b instanceof AndesiteBlock
            || b instanceof DioriteBlock) {
            return hasPickaxe;
        }
        if (b instanceof GravelBlock || b instanceof SandBlock
            || b instanceof ClayBlock) {
            return hasShovel || hasPickaxe;
        }
        return hard < 50f; // anything reasonably soft
    }

    public boolean isWater(BlockPos pos) {
        if (world == null) return false;
        FluidState f = world.getFluidState(pos);
        return f.isOf(Fluids.WATER) || f.isOf(Fluids.FLOWING_WATER);
    }

    public boolean isLava(BlockPos pos) {
        if (world == null) return false;
        FluidState f = world.getFluidState(pos);
        return f.isOf(Fluids.LAVA) || f.isOf(Fluids.FLOWING_LAVA);
    }

    public boolean isLadder(BlockPos pos) {
        if (world == null) return false;
        Block b = world.getBlockState(pos).getBlock();
        return b instanceof LadderBlock || b instanceof VineBlock;
    }

    public boolean isDangerous(BlockPos pos) {
        if (world == null) return false;
        Block b = world.getBlockState(pos).getBlock();
        return b instanceof LavaBlock
            || b instanceof FireBlock
            || b instanceof CactusBlock
            || b instanceof MagmaBlock
            || b instanceof SweetBerryBushBlock
            || isLava(pos);
    }
}
