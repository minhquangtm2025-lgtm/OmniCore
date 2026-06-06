package com.omnicore.module.movement;

import com.omnicore.module.Module;
import com.omnicore.module.ModuleCategory;
import com.omnicore.pathfinding.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.concurrent.*;

/**
 * Pathfinding module – equivalent to Baritone's IPathingBehavior.
 *
 * Flow (mirrors Baritone):
 *   1. goTo() / follow() sets a goal
 *   2. onTick() triggers async path calculation on a worker thread
 *   3. Once path is ready, PathExecutor walks it tick-by-tick
 *   4. If PathExecutor reports stuck/failed → recalculate from current pos
 *   5. Goal reached → disable
 *
 * Supports:
 *   - goto <x y z>       navigate to block coordinates
 *   - follow <entity>    continuously recompute toward a moving entity
 *   - allowDig           break blocks in the way (like Baritone's MineProcess)
 *   - allowParkour       leap over 1-block gaps
 */
public class PathfindingModule extends Module {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ── Settings ──────────────────────────────────────────────────────────────
    private boolean allowDig     = false;
    private boolean allowParkour = true;

    // ── Goal ──────────────────────────────────────────────────────────────────
    private BlockPos     goalPos    = null;
    private Entity       followEntity = null;
    private BlockPos     lastFollowGoal = null;
    private static final int FOLLOW_RECOMPUTE_DIST = 3; // recompute if entity moved > N blocks

    // ── Path state ────────────────────────────────────────────────────────────
    private PathExecutor           executor        = null;
    private Future<List<PathNode>> pendingPath     = null;
    private boolean                calculating     = false;
    private int                    recalcCooldown  = 0;
    private static final int       RECALC_DELAY    = 20; // ticks between recalc attempts

    private static final ExecutorService THREAD_POOL =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "OmniCore-Pathfinder");
            t.setDaemon(true);
            return t;
        });

    public PathfindingModule() {
        super("Pathfinding", "A* navigation with dig/swim/ladder/parkour – equivalent to Baritone",
              ModuleCategory.MOVEMENT);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Navigate to an absolute block position. */
    public void goTo(BlockPos target) {
        this.goalPos      = target;
        this.followEntity = null;
        executor          = null;
        pendingPath       = null;
        setEnabled(true);
    }

    /** Continuously follow an entity (re-paths when it moves). */
    public void follow(Entity entity) {
        this.followEntity    = entity;
        this.goalPos         = null;
        this.lastFollowGoal  = null;
        executor             = null;
        pendingPath          = null;
        setEnabled(true);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onDisable() {
        if (executor  != null) executor.releaseKeys();
        if (pendingPath != null) pendingPath.cancel(true);
        executor    = null;
        pendingPath = null;
        goalPos     = null;
        followEntity = null;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (recalcCooldown > 0) { recalcCooldown--; }

        // ── Update follow goal ────────────────────────────────────────────────
        if (followEntity != null) {
            if (followEntity.isRemoved()) { setEnabled(false); return; }
            BlockPos entityPos = followEntity.getBlockPos();
            boolean entityMoved = lastFollowGoal == null
                || lastFollowGoal.getManhattanDistance(entityPos) > FOLLOW_RECOMPUTE_DIST;
            if (entityMoved) {
                lastFollowGoal = entityPos;
                goalPos        = entityPos;
                // Cancel current path and recompute
                if (executor  != null) executor.releaseKeys();
                executor    = null;
                pendingPath = null;
            }
        }

        if (goalPos == null) { setEnabled(false); return; }

        // ── Check if goal reached ─────────────────────────────────────────────
        BlockPos playerPos = mc.player.getBlockPos();
        if (playerPos.getManhattanDistance(goalPos) <= 2) {
            if (followEntity == null) { // for follow mode stay active
                setEnabled(false);
                return;
            }
        }

        // ── Poll pending async calculation ────────────────────────────────────
        if (pendingPath != null && pendingPath.isDone()) {
            try {
                List<PathNode> nodes = pendingPath.get();
                pendingPath = null;
                calculating = false;
                if (!nodes.isEmpty()) {
                    executor = new PathExecutor(nodes);
                } else {
                    // Path not found – retry after delay
                    recalcCooldown = RECALC_DELAY * 3;
                }
            } catch (Exception e) {
                pendingPath = null;
                calculating = false;
                recalcCooldown = RECALC_DELAY;
            }
        }

        // ── Tick executor ─────────────────────────────────────────────────────
        if (executor != null) {
            boolean active = executor.tick();
            if (!active) {
                if (executor.isFailed() && recalcCooldown == 0) {
                    // Stuck or path invalidated – recalculate
                    executor = null;
                    recalcCooldown = RECALC_DELAY;
                } else if (executor.isFinished()) {
                    executor = null;
                    if (followEntity == null) { setEnabled(false); }
                }
            }
            return;
        }

        // ── Start async calculation ───────────────────────────────────────────
        if (!calculating && pendingPath == null && recalcCooldown == 0) {
            startAsyncCalc(playerPos, goalPos);
        }
    }

    // ── Async path calculation ────────────────────────────────────────────────

    private void startAsyncCalc(BlockPos start, BlockPos goal) {
        calculating = true;
        // Capture context on main thread before handing to worker
        PathContext ctx = new PathContext(allowDig, allowParkour);
        pendingPath = THREAD_POOL.submit(() -> {
            AStarPathfinder finder = new AStarPathfinder(ctx);
            return finder.findPath(start, goal);
        });
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setAllowDig(boolean v)     { this.allowDig     = v; }
    public void setAllowParkour(boolean v) { this.allowParkour = v; }

    public String getStatus() {
        if (!isEnabled())  return "IDLE";
        if (calculating)   return "CALCULATING";
        if (executor != null) return "EXECUTING (" + executor.getIndex()
                                     + "/" + executor.pathLength() + ")";
        return "WAITING";
    }
}
