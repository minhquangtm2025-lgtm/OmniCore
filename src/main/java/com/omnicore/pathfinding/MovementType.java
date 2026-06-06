package com.omnicore.pathfinding;

/**
 * Every way the player can move between two BlockPos positions.
 * Mirrors Baritone's individual Movement classes collapsed into an enum.
 */
public enum MovementType {
    WALK,            // flat cardinal/diagonal, no obstacle
    SPRINT,          // same as walk but sprint-speed cost bonus
    JUMP,            // step up exactly 1 block
    FALL,            // drop down 1-3 blocks (no damage)
    SWIM,            // inside water, any direction
    SWIM_UP,         // surface from water upward
    LADDER,          // climb ladder or vine up/down
    PARKOUR,         // leap over a 1-block horizontal gap (cardinal)
    DIG_HORIZONTAL,  // break 1-2 blocks to walk through
    DIG_DOWN,        // break block directly below (mine shaft)
    DIAGONAL         // 45-degree walk (slightly higher cost)
}
