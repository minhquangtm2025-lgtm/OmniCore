package com.omnicore.pathfinding;

import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * A* pathfinder with Baritone-equivalent movement types:
 * walk, sprint, jump, fall, swim, ladder, parkour, dig.
 *
 * Movement costs mirror Baritone's AbstractNodeCostSearch cost table.
 *
 * Cardinal  = N/S/E/W
 * Diagonal  = NE/NW/SE/SW (cost × √2)
 */
public class AStarPathfinder {

    // ── Cost constants (same logic as Baritone) ──────────────────────────────
    public static final double COST_WALK         = 1.0;
    public static final double COST_DIAGONAL     = 1.4142135623730951; // √2
    public static final double COST_SPRINT_BONUS = 0.17; // subtracted when sprinting
    public static final double COST_JUMP         = COST_WALK + 0.5;
    public static final double COST_FALL_1       = COST_WALK + 0.1;
    public static final double COST_FALL_PER     = 0.5;  // added per additional block
    public static final double COST_SWIM         = COST_WALK * 8.0;
    public static final double COST_SWIM_UP      = COST_WALK * 10.0;
    public static final double COST_LADDER       = COST_WALK * 5.0;
    public static final double COST_PARKOUR      = COST_WALK * 2.0;
    public static final double COST_BREAK        = COST_WALK * 10.0; // per block to break
    public static final double COST_BREAK_BONUS  = -2.0;  // reward for having right tool

    private static final int MAX_ITERATIONS  = 8000;
    private static final int MAX_FALL_HEIGHT = 3;

    // 4 cardinal + 4 diagonal offsets
    private static final int[] DX = { 1, -1,  0,  0,  1,  1, -1, -1 };
    private static final int[] DZ = { 0,  0,  1, -1,  1, -1,  1, -1 };

    private final PathContext ctx;

    public AStarPathfinder(PathContext ctx) {
        this.ctx = ctx;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public List<PathNode> findPath(BlockPos start, BlockPos goal) {
        if (ctx.world == null) return List.of();

        PriorityQueue<PathNode>  open    = new PriorityQueue<>();
        Map<BlockPos, PathNode>  openMap = new HashMap<>();
        Set<BlockPos>            closed  = new HashSet<>();

        PathNode startNode = new PathNode(start, null, 0.0, heuristic(start, goal), MovementType.WALK);
        open.add(startNode);
        openMap.put(start, startNode);

        int iter = 0;
        while (!open.isEmpty() && iter++ < MAX_ITERATIONS) {
            PathNode cur = open.poll();
            openMap.remove(cur.pos);
            closed.add(cur.pos);

            if (cur.pos.equals(goal) || cur.pos.getManhattanDistance(goal) <= 1) {
                return reconstructPath(cur);
            }

            for (Move move : generateMoves(cur.pos)) {
                if (closed.contains(move.dest)) continue;
                double newG = cur.g + move.cost;
                PathNode existing = openMap.get(move.dest);
                if (existing == null || newG < existing.g) {
                    PathNode node = new PathNode(
                        move.dest, cur, newG,
                        heuristic(move.dest, goal),
                        move.type, move.toBreak
                    );
                    open.add(node);
                    openMap.put(move.dest, node);
                }
            }
        }
        return List.of(); // no path found within iteration budget
    }

    // ── Move generation (one per movement type) ──────────────────────────────

    private List<Move> generateMoves(BlockPos src) {
        List<Move> moves = new ArrayList<>(24);

        for (int i = 0; i < 8; i++) {
            int nx = src.getX() + DX[i];
            int ny = src.getY();
            int nz = src.getZ() + DZ[i];
            boolean diagonal = i >= 4;
            double baseCost  = diagonal ? COST_DIAGONAL : COST_WALK;
            if (ctx.canSprint) baseCost -= COST_SPRINT_BONUS;

            BlockPos dest = new BlockPos(nx, ny, nz);

            // ── Walk flat ────────────────────────────────────────────────────
            if (ctx.isWalkable(dest)) {
                // Diagonal: check neither side-block is solid (no squeezing)
                if (!diagonal || diagonalClear(src, nx, ny, nz)) {
                    moves.add(new Move(dest, baseCost,
                        diagonal ? MovementType.DIAGONAL : MovementType.WALK));
                }
            }

            // ── Jump up 1 ────────────────────────────────────────────────────
            BlockPos up1 = new BlockPos(nx, ny + 1, nz);
            if (ctx.isWalkable(up1)
                    && ctx.isPassable(src.up())    // head clearance at src
                    && ctx.isPassable(src.up(2))) {
                if (!diagonal || diagonalClear(src, nx, ny + 1, nz)) {
                    moves.add(new Move(up1,
                        baseCost + COST_JUMP - COST_WALK,
                        diagonal ? MovementType.DIAGONAL : MovementType.JUMP));
                }
            }

            // ── Fall down 1-MAX_FALL_HEIGHT ───────────────────────────────────
            if (ctx.isPassable(dest)) {
                for (int fall = 1; fall <= MAX_FALL_HEIGHT; fall++) {
                    BlockPos landPos = new BlockPos(nx, ny - fall, nz);
                    if (!ctx.isPassable(landPos)) {
                        if (ctx.isWalkable(landPos) && !ctx.isDangerous(landPos)) {
                            double fallCost = baseCost + COST_FALL_1
                                + COST_FALL_PER * (fall - 1);
                            moves.add(new Move(landPos, fallCost, MovementType.FALL));
                        }
                        break; // hit ground (solid or dangerous)
                    }
                }
            }

            // ── Parkour: leap over 1-block gap (cardinal only) ──────────────
            if (!diagonal && ctx.allowParkour) {
                BlockPos gap = dest; // the gap block at same Y
                if (ctx.isPassable(gap) && ctx.isPassable(gap.up())) {
                    BlockPos land = new BlockPos(nx + DX[i], ny, nz + DZ[i]);
                    if (ctx.isWalkable(land)) {
                        moves.add(new Move(land, COST_PARKOUR, MovementType.PARKOUR));
                    }
                }
            }

            // ── Dig horizontal (cardinal only) ───────────────────────────────
            if (!diagonal && ctx.allowDig) {
                List<BlockPos> toBreak = new ArrayList<>();
                // Need to break dest body + dest head
                if (ctx.isBreakable(dest))        toBreak.add(dest);
                if (ctx.isBreakable(dest.up()))   toBreak.add(dest.up());
                // Stand position is walkable after digging
                if (!toBreak.isEmpty()
                        && ctx.isWalkable(dest.down())
                        && ctx.isPassable(dest.down().up())) {
                    double digCost = baseCost + COST_BREAK * toBreak.size();
                    if (ctx.hasPickaxe) digCost += COST_BREAK_BONUS;
                    moves.add(new Move(dest.down(), digCost,
                        MovementType.DIG_HORIZONTAL, toBreak));
                }
            }
        }

        // ── Swim (any direction while in water) ──────────────────────────────
        if (ctx.isWater(src)) {
            for (int i = 0; i < 4; i++) { // cardinal only for swimming
                BlockPos swimDest = new BlockPos(src.getX() + DX[i], src.getY(), src.getZ() + DZ[i]);
                if (!ctx.isDangerous(swimDest)) {
                    moves.add(new Move(swimDest, COST_SWIM, MovementType.SWIM));
                }
            }
            // Swim upward
            BlockPos swimUp = src.up();
            if (!ctx.isDangerous(swimUp)) {
                moves.add(new Move(swimUp, COST_SWIM_UP, MovementType.SWIM_UP));
            }
            // Exit water onto land
            for (int i = 0; i < 4; i++) {
                BlockPos land = new BlockPos(src.getX() + DX[i], src.getY() + 1, src.getZ() + DZ[i]);
                if (ctx.isWalkable(land)) {
                    moves.add(new Move(land, COST_SWIM + COST_JUMP, MovementType.SWIM));
                }
            }
        }

        // ── Ladder / vine ─────────────────────────────────────────────────────
        if (ctx.isLadder(src)) {
            moves.add(new Move(src.up(),   COST_LADDER, MovementType.LADDER));
            moves.add(new Move(src.down(), COST_LADDER, MovementType.LADDER));
        }
        // Enter ladder from adjacent block
        for (int i = 0; i < 4; i++) {
            BlockPos adj = new BlockPos(src.getX() + DX[i], src.getY(), src.getZ() + DZ[i]);
            if (ctx.isLadder(adj) && ctx.isPassable(adj)) {
                moves.add(new Move(adj, COST_LADDER, MovementType.LADDER));
            }
        }

        // ── Dig down ─────────────────────────────────────────────────────────
        if (ctx.allowDig) {
            BlockPos below = src.down();
            if (ctx.isBreakable(below) && !ctx.isDangerous(below)) {
                double digCost = COST_BREAK;
                if (ctx.hasPickaxe || ctx.hasShovel) digCost += COST_BREAK_BONUS;
                moves.add(new Move(below, digCost, MovementType.DIG_DOWN,
                    List.of(below)));
            }
        }

        return moves;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Baritone diagonal clearance: neither of the two side blocks may be solid. */
    private boolean diagonalClear(BlockPos src, int nx, int ny, int nz) {
        // The two "corner" blocks that would clip a diagonal move
        BlockPos side1 = new BlockPos(nx, ny, src.getZ());
        BlockPos side2 = new BlockPos(src.getX(), ny, nz);
        return ctx.isPassable(side1) && ctx.isPassable(side1.up())
            && ctx.isPassable(side2) && ctx.isPassable(side2.up());
    }

    /** Octile heuristic – admissible, matches Baritone's cost floor. */
    private double heuristic(BlockPos a, BlockPos b) {
        double dx = Math.abs(a.getX() - b.getX());
        double dy = Math.abs(a.getY() - b.getY());
        double dz = Math.abs(a.getZ() - b.getZ());
        double straight  = Math.abs(dx - dz);
        double diag      = Math.min(dx, dz);
        return (straight + diag * COST_DIAGONAL) * (COST_WALK - COST_SPRINT_BONUS)
             + dy * COST_FALL_1;
    }

    private List<PathNode> reconstructPath(PathNode node) {
        LinkedList<PathNode> path = new LinkedList<>();
        while (node != null) { path.addFirst(node); node = node.parent; }
        return path;
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    private record Move(BlockPos dest, double cost, MovementType type, List<BlockPos> toBreak) {
        Move(BlockPos dest, double cost, MovementType type) {
            this(dest, cost, type, List.of());
        }
    }
}
