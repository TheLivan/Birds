package com.thelivan.birds.client;

import java.util.Random;

import com.thelivan.birds.util.Vec3d;

/**
 * Shared heading for a group of birds with the same {@link ClientBird#flockId}; occasionally nudges its own
 * direction so the group doesn't fly dead straight forever.
 */
final class Flock {

    final long flockId;

    private final Random rng;
    private Vec3d groupForward;
    private int ticksToChange;

    Flock(long flockId, Vec3d initialForward) {
        this.flockId = flockId;
        this.rng = new Random(flockId);

        Vec3d gf = new Vec3d(initialForward.x, 0, initialForward.z).normalize();
        this.groupForward = (gf.lengthSquared() < 1e-6) ? new Vec3d(0, 0, 1) : gf;
        this.ticksToChange = 80 + rng.nextInt(180);
    }

    void tick() {
        if (--ticksToChange <= 0) {
            nudgeHeading();
            ticksToChange = 80 + rng.nextInt(220);
        }
    }

    private void nudgeHeading() {
        double ang = (rng.nextDouble() - 0.5) * Math.toRadians(35);
        double cos = Math.cos(ang);
        double sin = Math.sin(ang);

        double nx = groupForward.x * cos - groupForward.z * sin;
        double nz = groupForward.x * sin + groupForward.z * cos;
        groupForward = new Vec3d(nx, 0, nz).normalize();
    }

    Vec3d getGroupForward() {
        return groupForward;
    }
}
