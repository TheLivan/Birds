package com.thelivan.birds.client;

import java.util.Collection;
import java.util.Random;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.thelivan.birds.client.sound.BirdCallType;
import com.thelivan.birds.client.sound.BirdSoundSystem;
import com.thelivan.birds.client.util.BirdOrientation;
import com.thelivan.birds.util.Vec3d;

public class ClientBird {

    public final BirdSpecies species;
    public final BirdOrientation orientation = new BirdOrientation();
    public final ResourceLocation texture;

    private final long birdSeed;

    public int ageTicks = 0;
    public Vec3d pos;
    public Vec3d vel;
    public long flockId = 0L;
    public Vec3d prevPos;
    public float prevYaw, prevPitch, prevRoll;

    private Vec3d lastForwardXZ = new Vec3d(0, 0, 1);

    private final Random callRandom;
    private int nextSingleCallTick = Integer.MIN_VALUE;
    private int nextFlockCallTick = Integer.MIN_VALUE;

    private enum Mode {
        GLIDE,
        CIRCLE
    }

    private final Random flightRandom;
    private Mode mode;
    private int modeTicksLeft;
    private Vec3d waypoint;
    private Vec3d circleCenter;
    private double circleRadius;

    public ClientBird(BirdSpecies species, long birdSeed, Vec3d startPos, Vec3d initialDir, double speed) {
        this.species = species;
        this.birdSeed = birdSeed;
        this.callRandom = new Random(birdSeed ^ 0xC411L);
        this.flightRandom = new Random(birdSeed ^ 0x9E3779B1L);

        this.pos = startPos;
        this.prevPos = startPos;

        Vec3d forward = initialDir.normalize();
        if (forward.lengthSquared() < 1e-6) forward = new Vec3d(0, 0, 1);
        this.vel = forward.scale(speed);
        this.lastForwardXZ = new Vec3d(forward.x, 0, forward.z).normalize();

        this.texture = (species != null) ? species.pickTexture(birdSeed) : null;

        pickNewMode();
    }

    public void tick(World world, Vec3d flockForward, Collection<ClientBird> neighbors) {
        if (world == null) return;

        prevPos = pos;
        prevYaw = orientation.yawDeg;
        prevPitch = orientation.pitchDeg;
        prevRoll = orientation.rollDeg;

        ageTicks++;

        double maxTurnDeg = (species != null) ? species.maxTurnDegPerTick : 4.0;

        if (flockId != 0L) {
            vel = tickFlockHeading(flockForward, neighbors, maxTurnDeg);
        } else {
            vel = tickSoloHeading(maxTurnDeg);
        }

        vel = avoidObstacles(world, maxTurnDeg);

        if (species != null) {
            double vy = verticalVelocity(world, species.viewForTime(world.isDaytime()));
            vel = new Vec3d(vel.x, vy, vel.z);
        }

        Vec3d fNow = new Vec3d(vel.x, 0, vel.z).normalize();
        Vec3d fPrev = lastForwardXZ;
        double cross = (fPrev.x * fNow.z) - (fPrev.z * fNow.x); // signed turn amount, drives banking
        float targetRoll = (float) clamp(-cross * 55.0, -35.0, 35.0);
        orientation.setTargetRoll(targetRoll, 3.0f);

        orientation.updateFromVelocity(vel, 6.0f, 4.0f, 3.0f);

        lastForwardXZ = fNow;

        pos = pos.add(vel);

        tickCalls(world);
    }

    private Vec3d tickFlockHeading(Vec3d flockForward, Collection<ClientBird> neighbors, double maxTurnDeg) {
        Vec3d currentXZ = new Vec3d(vel.x, 0, vel.z);
        double speed = currentXZ.length();
        if (speed < 1e-6) speed = (species != null) ? species.minSpeed : 0.4;

        Vec3d desired = (flockForward != null && flockForward.lengthSquared() > 1e-8)
            ? new Vec3d(flockForward.x, 0, flockForward.z).normalize()
            : (currentXZ.lengthSquared() > 1e-8 ? currentXZ.normalize() : new Vec3d(0, 0, 1));

        if (neighbors != null) {
            Vec3d boids = FlockingRules.boidsSteer(this, neighbors);
            if (boids.lengthSquared() > 1e-8) desired = desired.add(boids).normalize();
        }

        Vec3d currentDir = (currentXZ.lengthSquared() > 1e-8) ? currentXZ.normalize() : desired;
        Vec3d newDir = limitTurnXZ(currentDir, desired, Math.toRadians(maxTurnDeg));

        Vec3d scaled = newDir.scale(speed);
        return new Vec3d(scaled.x, vel.y, scaled.z);
    }

    private static Vec3d limitTurnXZ(Vec3d currentForward, Vec3d desiredForward, double maxTurnRad) {
        Vec3d c = currentForward.lengthSquared() > 1e-8 ? currentForward.normalize() : new Vec3d(0, 0, 1);
        Vec3d d = desiredForward.lengthSquared() > 1e-8 ? desiredForward.normalize() : new Vec3d(0, 0, 1);

        double dot = clamp(c.dotProduct(d), -1.0, 1.0);
        double angle = Math.acos(dot);
        if (angle <= maxTurnRad) return d;

        double t = maxTurnRad / angle;
        return c.scale(1 - t)
            .add(d.scale(t))
            .normalize();
    }

    private static final double OBSTACLE_LOOKAHEAD = 16.0;

    // Steer side is fixed per bird (seeded), not picked by which way is actually clearer.
    private Vec3d avoidObstacles(World world, double maxTurnDeg) {
        Vec3d dirXZ = new Vec3d(vel.x, 0, vel.z);
        if (dirXZ.lengthSquared() < 1e-6) return vel;
        dirXZ = dirXZ.normalize();

        Vec3 start = Vec3.createVectorHelper(pos.x, pos.y, pos.z);
        Vec3d ahead = pos.add(dirXZ.scale(OBSTACLE_LOOKAHEAD));
        Vec3 end = Vec3.createVectorHelper(ahead.x, ahead.y, ahead.z);

        MovingObjectPosition hit = world.rayTraceBlocks(start, end);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return vel;

        boolean steerLeft = (birdSeed & 2L) == 0L;
        Vec3d side = steerLeft ? new Vec3d(-dirXZ.z, 0, dirXZ.x) : new Vec3d(dirXZ.z, 0, -dirXZ.x);
        Vec3d avoidDir = dirXZ.add(side.scale(0.8)).normalize();

        Vec3d newDir = limitTurnXZ(dirXZ, avoidDir, Math.toRadians(maxTurnDeg * 1.25));

        Vec3d scaled = newDir.scale(new Vec3d(vel.x, 0, vel.z).length());
        return new Vec3d(scaled.x, vel.y, scaled.z);
    }

    private Vec3d tickSoloHeading(double maxTurnDeg) {
        if (modeTicksLeft-- <= 0) pickNewMode();

        Vec3d desired = desiredSoloDirection();

        double noise = (species != null) ? species.noiseStrength : 0.04;
        desired = desired.add((flightRandom.nextDouble() - 0.5) * noise, 0, (flightRandom.nextDouble() - 0.5) * noise);
        if (desired.lengthSquared() > 1e-8) desired = desired.normalize();

        Vec3d currentXZ = new Vec3d(vel.x, 0, vel.z);
        Vec3d currentDir = (currentXZ.lengthSquared() > 1e-8) ? currentXZ.normalize() : desired;
        Vec3d newDir = limitTurnXZ(currentDir, desired, Math.toRadians(maxTurnDeg));

        double minSpeed = (species != null) ? species.minSpeed : 0.35;
        double maxSpeed = (species != null) ? species.maxSpeed : 0.6;
        double targetSpeed = (mode == Mode.CIRCLE) ? lerp(minSpeed, maxSpeed, 0.35) : lerp(minSpeed, maxSpeed, 0.65);

        double speed = currentXZ.length();
        if (speed < 1e-6) speed = minSpeed;
        speed = lerp(speed, targetSpeed, 0.03);

        Vec3d scaled = newDir.scale(speed);
        return new Vec3d(scaled.x, vel.y, scaled.z);
    }

    private Vec3d desiredSoloDirection() {
        if (mode == Mode.GLIDE) {
            Vec3d to = new Vec3d(waypoint.x - pos.x, 0, waypoint.z - pos.z);
            if (to.lengthSquared() < 16.0) {
                pickGlideWaypoint();
                to = new Vec3d(waypoint.x - pos.x, 0, waypoint.z - pos.z);
            }
            return to.normalize();
        }

        Vec3d toCenter = new Vec3d(circleCenter.x - pos.x, 0, circleCenter.z - pos.z);
        Vec3d radial = (toCenter.lengthSquared() > 1e-8) ? toCenter.normalize() : new Vec3d(1, 0, 0);

        boolean clockwise = (birdSeed & 1L) == 0L;
        Vec3d tangent = clockwise ? new Vec3d(-radial.z, 0, radial.x) : new Vec3d(radial.z, 0, -radial.x);

        double dx = pos.x - circleCenter.x;
        double dz = pos.z - circleCenter.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double err = circleRadius - dist;
        Vec3d correction = radial.scale(-err * 0.02);

        return tangent.add(correction)
            .normalize();
    }

    private void pickNewMode() {
        double weightGlide = (species != null) ? species.patternWeightGlide : 0.55;
        double weightCircle = (species != null) ? species.patternWeightCircle : 0.45;
        double sum = weightGlide + weightCircle;
        if (sum <= 0) {
            weightGlide = 1.0;
            sum = 1.0;
        }

        boolean chooseCircle = flightRandom.nextDouble() * sum >= weightGlide;

        if (chooseCircle) {
            mode = Mode.CIRCLE;
            modeTicksLeft = randInt(
                (species != null) ? species.circleMinTicks : 80,
                (species != null) ? species.circleMaxTicks : 220);
            circleRadius = lerp(
                (species != null) ? species.circleRadiusMin : 16.0,
                (species != null) ? species.circleRadiusMax : 64.0,
                flightRandom.nextDouble());

            double ang = flightRandom.nextDouble() * Math.PI * 2.0;
            circleCenter = new Vec3d(pos.x + Math.cos(ang) * circleRadius, pos.y, pos.z + Math.sin(ang) * circleRadius);
        } else {
            mode = Mode.GLIDE;
            modeTicksLeft = randInt(
                (species != null) ? species.glideMinTicks : 60,
                (species != null) ? species.glideMaxTicks : 140);
            pickGlideWaypoint();
        }
    }

    private void pickGlideWaypoint() {
        Vec3d fwdXZ = new Vec3d(vel.x, 0, vel.z);
        Vec3d dirXZ = (fwdXZ.lengthSquared() > 1e-8) ? fwdXZ.normalize() : new Vec3d(0, 0, 1);

        double dist = 80 + flightRandom.nextDouble() * 140;
        double ang = Math.atan2(dirXZ.z, dirXZ.x) + (flightRandom.nextDouble() - 0.5) * Math.toRadians(50);

        double wx = pos.x + Math.cos(ang) * dist;
        double wz = pos.z + Math.sin(ang) * dist;

        waypoint = new Vec3d(wx, pos.y, wz); // y unused, verticalVelocity() drives altitude on its own
    }

    private int randInt(int lo, int hi) {
        if (hi <= lo) return lo;
        return lo + flightRandom.nextInt(hi - lo + 1);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static final double[] TERRAIN_LOOKAHEAD = { 6, 12, 18, 26, 34 };

    private double verticalVelocity(World world, BirdSpecies.BirdSpeciesView view) {
        double groundHereY = world.getHeightValue((int) Math.floor(pos.x), (int) Math.floor(pos.z));
        double groundAheadY = maxGroundAhead(world, groundHereY);

        double minAbove = view.minAltitudeAboveGround();
        double maxAbove = view.maxAltitudeAboveGround();
        double preferredAbove = clamp(view.preferredAboveGround(), minAbove, maxAbove);

        // floor looks ahead (climbs before a slope, not after); target/ceiling follow the ground right below
        double floorY = groundAheadY + minAbove;
        double ceilingY = groundHereY + maxAbove;
        double targetY = groundHereY + preferredAbove;

        if (pos.y < floorY + 6.0) {
            targetY = Math.max(targetY, floorY + 6.0);
        } else if (pos.y > ceilingY - 6.0) {
            targetY = Math.min(targetY, ceilingY - 6.0);
        }

        double yError = targetY - pos.y;
        double vy = clamp(yError * view.verticalAdjustStrength(), -0.06, 0.06);

        if (pos.y + vy < floorY) {
            vy = Math.min(0.12, floorY - pos.y);
        }

        return vy;
    }

    private double maxGroundAhead(World world, double fallback) {
        Vec3d dirXZ = new Vec3d(vel.x, 0, vel.z);
        if (dirXZ.lengthSquared() < 1e-6) return fallback;
        dirXZ = dirXZ.normalize();

        double maxGround = fallback;
        for (double d : TERRAIN_LOOKAHEAD) {
            int ax = (int) Math.floor(pos.x + dirXZ.x * d);
            int az = (int) Math.floor(pos.z + dirXZ.z * d);
            double g = world.getHeightValue(ax, az);
            if (g > maxGround) maxGround = g;
        }
        return maxGround;
    }

    private void tickCalls(World world) {
        if (species == null) return;

        BirdSpecies.BirdSpeciesView view = species.viewForTime(world.isDaytime());
        if (!view.soundsEnabled()) return;

        int tick = (int) world.getTotalWorldTime();

        nextSingleCallTick = tickCall(BirdCallType.SINGLE, view.sound(BirdCallType.SINGLE), tick, nextSingleCallTick);

        if (flockId != 0L) {
            nextFlockCallTick = tickCall(BirdCallType.FLOCK, view.sound(BirdCallType.FLOCK), tick, nextFlockCallTick);
        }
    }

    private int tickCall(BirdCallType type, BirdSpecies.SoundView sound, int tick, int nextCallTick) {
        if (nextCallTick == Integer.MIN_VALUE) {
            return tick + jitteredInterval(sound);
        }

        if (tick < nextCallTick) return nextCallTick;

        BirdSoundSystem.playCall(this, type, sound);
        return tick + jitteredInterval(sound);
    }

    private int jitteredInterval(BirdSpecies.SoundView sound) {
        int base = sound.soundBaseIntervalTicks();
        double jitter = (callRandom.nextDouble() * 2.0 - 1.0) * sound.soundRandomness();
        int interval = (int) Math.round(base * (1.0 + jitter));
        return Math.max(20, interval);
    }

    public long getId() {
        return birdSeed;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
