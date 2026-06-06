package com.omnicore.module.building;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.*;

public class SchematicBuilderModule extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Map of relative BlockPos -> BlockState to place
    private final List<Map.Entry<BlockPos, Block>> buildQueue = new ArrayList<>();
    private BlockPos origin = BlockPos.ORIGIN;
    private int buildIndex = 0;
    private int actionCooldown = 0;

    public SchematicBuilderModule() {
        super("SchematicBuilder", "Auto-build structures from a block list", ModuleCategory.BUILDING);
    }

    /**
     * Load a flat list of relative positions and block identifiers.
     * Example: loadSchematic(BlockPos.ORIGIN, Map.of(new BlockPos(0,0,0), "minecraft:stone"))
     */
    public void loadSchematic(BlockPos originPos, Map<BlockPos, String> blockMap) {
        buildQueue.clear();
        buildIndex = 0;
        this.origin = originPos;
        for (Map.Entry<BlockPos, String> entry : blockMap.entrySet()) {
            Block block = Registries.BLOCK.get(Identifier.of(entry.getValue()));
            buildQueue.add(Map.entry(entry.getKey(), block));
        }
        setEnabled(true);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (buildIndex >= buildQueue.size()) { setEnabled(false); return; }
        if (actionCooldown > 0) { actionCooldown--; return; }

        Map.Entry<BlockPos, Block> entry = buildQueue.get(buildIndex);
        BlockPos worldPos = origin.add(entry.getKey());
        Block targetBlock = entry.getValue();

        // Skip if already placed
        if (mc.world.getBlockState(worldPos).getBlock() == targetBlock) {
            buildIndex++;
            return;
        }

        // Too far away - skip for now (pathfinding integration would move player)
        if (mc.player.getPos().distanceTo(Vec3d.ofCenter(worldPos)) > 5) {
            buildIndex++;
            return;
        }

        // Find the block in hotbar
        int slot = findHotbarSlot(targetBlock);
        if (slot == -1) { buildIndex++; return; } // missing material

        mc.player.getInventory().selectedSlot = slot;

        // Place on the block below
        BlockHitResult hit = new BlockHitResult(
            Vec3d.ofCenter(worldPos.down()), Direction.UP, worldPos.down(), false
        );
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        buildIndex++;
        actionCooldown = 3;
    }

    private int findHotbarSlot(Block block) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi
                && bi.getBlock() == block) return i;
        }
        return -1;
    }
}
