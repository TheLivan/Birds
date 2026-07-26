package com.thelivan.birds.client;

import java.util.Collection;

import com.thelivan.birds.util.Vec3d;

/** Simple boids (cohesion/alignment/separation) among same-flock neighbors. */
final class FlockingRules {

    private static final double NEIGHBOR_RADIUS = 48.0;
    private static final double SEPARATION_RADIUS = 6.0;

    private static final double WEIGHT_COHESION = 1.35;
    private static final double WEIGHT_ALIGNMENT = 1.05;
    private static final double WEIGHT_SEPARATION = 0.65;

    private static final double MAX_FORCE = 0.10;

    private FlockingRules() {}

    static Vec3d boidsSteer(ClientBird self, Collection<ClientBird> candidates) {
        Vec3d cohesion = Vec3d.ZERO;
        Vec3d alignment = Vec3d.ZERO;
        Vec3d separation = Vec3d.ZERO;
        int count = 0;

        for (ClientBird other : candidates) {
            if (other == self || other.flockId != self.flockId) continue;

            double d2 = self.pos.squareDistanceTo(other.pos);
            if (d2 > NEIGHBOR_RADIUS * NEIGHBOR_RADIUS) continue;

            count++;
            cohesion = cohesion.add(other.pos);
            alignment = alignment.add(other.vel);

            if (d2 < SEPARATION_RADIUS * SEPARATION_RADIUS && d2 > 1e-6) {
                Vec3d away = self.pos.subtract(other.pos)
                    .normalize()
                    .scale(1.0 / Math.sqrt(d2));
                separation = separation.add(away);
            }
        }

        if (count == 0) return Vec3d.ZERO;

        cohesion = cohesion.scale(1.0 / count)
            .subtract(self.pos);
        if (cohesion.lengthSquared() > 1e-8) cohesion = cohesion.normalize();

        if (alignment.lengthSquared() > 1e-8) alignment = alignment.normalize();
        if (separation.lengthSquared() > 1e-8) separation = separation.normalize();

        Vec3d force = cohesion.scale(WEIGHT_COHESION)
            .add(alignment.scale(WEIGHT_ALIGNMENT))
            .add(separation.scale(WEIGHT_SEPARATION));

        if (force.lengthSquared() > MAX_FORCE * MAX_FORCE) {
            force = force.normalize()
                .scale(MAX_FORCE);
        }

        return force;
    }
}
