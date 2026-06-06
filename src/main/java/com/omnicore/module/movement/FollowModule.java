package com.omnicore.module.movement;

import com.omnicore.OmniCore;
import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Follow a player or entity by name/nearest.
 * Delegates all pathfinding to PathfindingModule (like Baritone's FollowProcess).
 */
public class FollowModule extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private String  targetName  = null;  // player name; null = nearest entity
    private double  stopRange   = 3.0;   // stop following when this close
    private Entity  target      = null;

    public FollowModule() {
        super("Follow", "Follow a player or entity using pathfinding", ModuleCategory.MOVEMENT);
    }

    /** Follow nearest entity within 64 blocks. */
    public void followNearest() {
        this.targetName = null;
        setEnabled(true);
    }

    /** Follow a player by name. */
    public void followPlayer(String name) {
        this.targetName = name;
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        target = resolveTarget();
        if (target == null) {
            setEnabled(false);
            return;
        }
        PathfindingModule pfm = getPathfindingModule();
        if (pfm != null) pfm.follow(target);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;

        // Re-resolve if target gone
        if (target == null || (target instanceof LivingEntity le && !le.isAlive())) {
            target = resolveTarget();
            if (target == null) { setEnabled(false); return; }
            PathfindingModule pfm = getPathfindingModule();
            if (pfm != null) pfm.follow(target);
        }

        // Stop if close enough
        if (mc.player.distanceTo(target) <= stopRange) {
            PathfindingModule pfm = getPathfindingModule();
            if (pfm != null) pfm.setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        PathfindingModule pfm = getPathfindingModule();
        if (pfm != null) pfm.setEnabled(false);
        target = null;
    }

    private Entity resolveTarget() {
        if (mc.world == null || mc.player == null) return null;

        if (targetName != null) {
            // Find by player name
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p != mc.player && p.getName().getString().equalsIgnoreCase(targetName)) return p;
            }
            return null;
        }

        // Nearest living entity
        Box box = mc.player.getBoundingBox().expand(64);
        List<LivingEntity> entities = mc.world.getEntitiesByClass(LivingEntity.class, box,
            e -> e != mc.player && e.isAlive());
        return entities.stream()
            .min(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
            .orElse(null);
    }

    private PathfindingModule getPathfindingModule() {
        return OmniCore.getInstance().getModuleManager()
            .getModule(PathfindingModule.class).orElse(null);
    }

    public void setStopRange(double r) { this.stopRange = r; }
}
