package com.omnicore.pathfinding;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Executes a computed path tick-by-tick.
 * Equivalent to Baritone's PathExecutor + MovementHelper combined.
 *
 * Responsibilities:
 * - Walk / sprint toward next waypoint
 * - Jump when next node is 1 block higher
 * - Fall / drop down safely
 * - Swim to surface
 * - Climb ladders
 * - Break blocks before entering (DIG moves)
 * - Detect stuck state and trigger recalculation
 * - Detect goal reached
 */
public class PathExecutor {

    // ── Tolerances ────────────────────────────────────────────────────────────
    private static final double REACH_DIST      = 0.35; // horizontal dist to advance waypoint
    private static final double REACH_DIST_SWIM = 0.6;
    private static final int    STUCK_THRESHOLD = 60;   // ticks without advancing = stuck
    private static final double STUCK_DIST_MIN  = 0.1;  // must move this far per STUCK_THRESHOLD

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<PathNode> path;
    private int     index       = 0;
    private int     stuckTicks  = 0;
    private Vec3d   lastPos     = Vec3d.ZERO;
    private boolean finished    = false;
    private boolean failed      = false;
    private boolean digInProgress = false;
    private int     digTicks    = 0;

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public PathExecutor(List<PathNode> path) {
        this.path = path;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Call every client tick. Returns false when the path is done or failed. */
    public boolean tick() {
        if (finished || failed) return false;
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null) { failed = true; return false; }
        if (index >= path.size()) { finished = true; releaseKeys(); return false; }

        PathNode node = path.get(index);

        // ── 1. Break blocks if this move requires digging ─────────────────────
        if (!node.toBreak.isEmpty() && !allBroken(node.toBreak)) {
            executeDig(node.toBreak);
            trackStuck(player);
            return true;
        }
        digInProgress = false;

        // ── 2. Execute movement toward the node ───────────────────────────────
        Vec3d target = Vec3d.ofCenter(node.pos);
        Vec3d pos    = player.getPos();

        double reachDist = node.moveType == MovementType.SWIM ? REACH_DIST_SWIM : REACH_DIST;
        double hDist = horizontalDist(pos, target);

        if (hDist < reachDist && Math.abs(pos.y - target.y) < 1.2) {
            // Waypoint reached – advance
            index++;
            stuckTicks = 0;
            lastPos    = pos;
            return true;
        }

        switch (node.moveType) {
            case WALK, SPRINT, DIAGONAL, JUMP -> executeWalk(player, target, node);
            case FALL                          -> executeFall(player, target);
            case SWIM, SWIM_UP                 -> executeSwim(player, target, node);
            case LADDER                        -> executeLadder(player, node);
            case PARKOUR                       -> executeParkour(player, target, node);
            case DIG_HORIZONTAL, DIG_DOWN      -> executeWalk(player, target, node);
        }

        trackStuck(player);
        return true;
    }

    public boolean isFinished() { return finished; }
    public boolean isFailed()   { return failed;   }
    public int     getIndex()   { return index;     }
    public int     pathLength() { return path.size(); }

    // ── Movement implementations ──────────────────────────────────────────────

    private void executeWalk(ClientPlayerEntity player, Vec3d target, PathNode node) {
        faceToward(player, target);
        player.input.movementForward = 1.0f;

        // Sprint: always try to sprint unless about to parkour (need precision)
        boolean doSprint = node.moveType != MovementType.PARKOUR;
        mc.options.sprintKey.setPressed(doSprint);

        // Jump if next block is higher
        if (node.pos.getY() > player.getBlockPos().getY() && player.isOnGround()) {
            player.jump();
        }

        // Jump over flat obstacles (horizontal collision while on ground)
        if (player.horizontalCollision && player.isOnGround()) {
            player.jump();
        }
    }

    private void executeFall(ClientPlayerEntity player, Vec3d target) {
        faceToward(player, target);
        player.input.movementForward = 0.3f; // gentle push into the drop
        mc.options.sprintKey.setPressed(false);
        // Sneak at the edge to control the drop (like Baritone)
        mc.options.sneakKey.setPressed(false);
    }

    private void executeSwim(ClientPlayerEntity player, Vec3d target, PathNode node) {
        faceToward(player, target);
        player.input.movementForward = 1.0f;
        mc.options.sprintKey.setPressed(true);

        // Hold jump to swim upward / stay at surface
        if (node.moveType == MovementType.SWIM_UP || player.isTouchingWater()) {
            mc.options.jumpKey.setPressed(true);
        }
    }

    private void executeLadder(ClientPlayerEntity player, PathNode node) {
        player.input.movementForward = 0.0f;
        mc.options.sprintKey.setPressed(false);

        if (node.pos.getY() > player.getBlockPos().getY()) {
            // Climbing up: hold forward + look at ladder face
            faceToward(player, Vec3d.ofCenter(node.pos));
            player.input.movementForward = 1.0f;
        } else {
            // Climbing down: hold sneak to descend slowly
            mc.options.sneakKey.setPressed(true);
        }
    }

    private void executeParkour(ClientPlayerEntity player, Vec3d target, PathNode node) {
        faceToward(player, target);
        // Sprint-jump: must be sprinting and jump exactly at the edge
        mc.options.sprintKey.setPressed(true);
        player.input.movementForward = 1.0f;
        if (player.isOnGround()) {
            player.jump();
        }
    }

    // ── Block breaking ────────────────────────────────────────────────────────

    private void executeDig(List<BlockPos> blocks) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!digInProgress) { digTicks = 0; digInProgress = true; }
        digTicks++;

        // Pick the first unbroken block and attack it
        for (BlockPos bp : blocks) {
            if (!mc.world.getBlockState(bp).isAir()) {
                // Face the block
                float[] rot = rotToBlock(bp);
                mc.player.setYaw(rot[0]);
                mc.player.setPitch(rot[1]);
                mc.interactionManager.updateBlockBreakingProgress(bp,
                    net.minecraft.util.math.Direction.DOWN);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
    }

    private boolean allBroken(List<BlockPos> blocks) {
        if (mc.world == null) return true;
        return blocks.stream().allMatch(bp -> mc.world.getBlockState(bp).isAir());
    }

    // ── Stuck detection ───────────────────────────────────────────────────────

    private void trackStuck(ClientPlayerEntity player) {
        Vec3d cur = player.getPos();
        stuckTicks++;
        if (stuckTicks >= STUCK_THRESHOLD) {
            double moved = cur.distanceTo(lastPos);
            if (moved < STUCK_DIST_MIN) {
                failed = true; // signal caller to recalculate
                releaseKeys();
            }
            stuckTicks = 0;
            lastPos    = cur;
        }
    }

    // ── Key / input cleanup ───────────────────────────────────────────────────

    public void releaseKeys() {
        if (mc.player == null) return;
        mc.player.input.movementForward  = 0;
        mc.player.input.movementSideways = 0;
        mc.options.sprintKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
    }

    // ── Rotation helpers ──────────────────────────────────────────────────────

    private void faceToward(ClientPlayerEntity player, Vec3d target) {
        float[] rot = rotToPos(player.getEyePos(), target);
        // Smooth rotation (like Baritone – not instant snap)
        float curYaw   = player.getYaw();
        float curPitch = player.getPitch();
        float dYaw   = wrapDeg(rot[0] - curYaw);
        float dPitch = rot[1] - curPitch;
        player.setYaw(curYaw   + MathHelper.clamp(dYaw,   -20f, 20f));
        player.setPitch(curPitch + MathHelper.clamp(dPitch, -15f, 15f));
    }

    private float[] rotToPos(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double dh = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dh));
        return new float[]{ yaw, pitch };
    }

    private float[] rotToBlock(BlockPos bp) {
        if (mc.player == null) return new float[]{0, 0};
        return rotToPos(mc.player.getEyePos(), Vec3d.ofCenter(bp));
    }

    private float wrapDeg(float d) {
        d %= 360f;
        if (d >= 180f)  d -= 360f;
        if (d < -180f)  d += 360f;
        return d;
    }

    private double horizontalDist(Vec3d a, Vec3d b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
