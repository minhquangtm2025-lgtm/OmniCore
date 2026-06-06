package com.omnicore.pathfinding;

import net.minecraft.util.math.BlockPos;
import java.util.List;

public class PathNode implements Comparable<PathNode> {
    public final BlockPos pos;
    public PathNode parent;
    public double g;
    public double h;
    public double f;
    public final MovementType moveType;
    public final List<BlockPos> toBreak; // blocks to mine for this move

    public PathNode(BlockPos pos, PathNode parent, double g, double h,
                    MovementType moveType, List<BlockPos> toBreak) {
        this.pos      = pos;
        this.parent   = parent;
        this.g        = g;
        this.h        = h;
        this.f        = g + h;
        this.moveType = moveType;
        this.toBreak  = toBreak != null ? toBreak : List.of();
    }

    public PathNode(BlockPos pos, PathNode parent, double g, double h, MovementType moveType) {
        this(pos, parent, g, h, moveType, List.of());
    }

    @Override public int compareTo(PathNode o) { return Double.compare(f, o.f); }
    @Override public boolean equals(Object o) {
        return o instanceof PathNode n && pos.equals(n.pos);
    }
    @Override public int hashCode() { return pos.hashCode(); }
}
