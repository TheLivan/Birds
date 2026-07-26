package com.thelivan.birds.client;

import java.util.List;
import java.util.Random;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.thelivan.birds.client.sound.BirdCallType;
import com.thelivan.birds.client.sound.BirdSoundSystem;
import com.thelivan.birds.client.util.BirdOrientation;
import com.thelivan.birds.util.Vec3d;

/**
 * A single client-side bird.
 * <p>
 * Fields and signatures are kept identical to the 1.12.2 original so that porting the real flight logic later only
 * replaces the body of {@link #tick(World, Vec3d, List)}.
 */
public class ClientBird {

    public final BirdSpecies species;
    public final BirdOrientation orientation = new BirdOrientation();

    /**
     * Deterministic chosen texture for this bird
     */
    public final ResourceLocation texture;

    private final long birdSeed;

    public int ageTicks = 0;
    public Vec3d pos;
    public Vec3d vel;
    public long flockId = 0L;
    public Vec3d prevPos;
    public float prevYaw, prevPitch, prevRoll;

    /**
     * used for banking (roll)
     */
    private Vec3d lastForwardXZ = new Vec3d(0, 0, 1);

    private final Random callRandom;
    private int nextSingleCallTick = Integer.MIN_VALUE;
    private int nextFlockCallTick = Integer.MIN_VALUE;

    /**
     * @param world unused by the placeholder tick, kept so the signature matches the original constructor
     */
    public ClientBird(World world, BirdSpecies species, long birdSeed, Vec3d startPos, Vec3d initialDir, double speed) {
        this.species = species;
        this.birdSeed = birdSeed;
        this.callRandom = new Random(birdSeed ^ 0xC411L);

        this.pos = startPos;
        this.prevPos = startPos;

        Vec3d forward = initialDir.normalize();
        if (forward.lengthSquared() < 1e-6) forward = new Vec3d(0, 0, 1);
        this.vel = forward.scale(speed);
        this.lastForwardXZ = new Vec3d(forward.x, 0, forward.z).normalize();

        // pick deterministic texture variation for this bird
        this.texture = (species != null) ? species.pickTexture(birdSeed) : null;
    }

    /**
     * PLACEHOLDER: solo birds do a gentle deterministic turn so orientation, banking and interpolation can be
     * verified on screen. Flock members (see {@link #flockId}) instead steer toward the flock's shared heading plus
     * local boids (cohesion/alignment/separation, {@link FlockingRules}). The real behaviour (glide/circle modes,
     * terrain avoidance) is still ported separately.
     *
     * @param flockForward the current flock's shared heading, or {@code null} for solo birds
     * @param neighbors    candidate birds to steer relative to (only same-{@link #flockId} ones are used), or
     *                     {@code null} for solo birds
     */
    public void tick(World world, Vec3d flockForward, List<ClientBird> neighbors) {
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
            double phase = (ageTicks + (birdSeed & 255L)) * 0.02;
            double turnRad = Math.toRadians(maxTurnDeg) * Math.sin(phase);

            double cos = Math.cos(turnRad);
            double sin = Math.sin(turnRad);
            vel = new Vec3d(vel.x * cos - vel.z * sin, vel.y, vel.x * sin + vel.z * cos);
        }

        if (species != null) {
            double vy = verticalVelocity(world, species.viewForTime(world.isDaytime()));
            vel = new Vec3d(vel.x, vy, vel.z);
        }

        // Banking roll: based on change in forward direction (turning)
        Vec3d fNow = new Vec3d(vel.x, 0, vel.z).normalize();
        Vec3d fPrev = lastForwardXZ;
        double cross = (fPrev.x * fNow.z) - (fPrev.z * fNow.x); // signed turn amount
        float targetRoll = (float) clamp(-cross * 55.0, -35.0, 35.0);
        orientation.setTargetRoll(targetRoll, 3.0f);

        orientation.updateFromVelocity(vel, 6.0f, 4.0f, 3.0f);

        lastForwardXZ = fNow;

        pos = pos.add(vel);

        tickCalls(world);
    }

    /**
     * Blends the current heading toward the flock's shared direction plus local boids steering, turn-rate limited
     * like the solo path. Speed magnitude is preserved; only the horizontal direction changes here.
     */
    private Vec3d tickFlockHeading(Vec3d flockForward, List<ClientBird> neighbors, double maxTurnDeg) {
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

    /**
     * Steers {@code pos.y} toward {@code ground + preferredAboveGround}, clamped to the species' altitude band, and
     * forces a climb if it's about to drop below the minimum. Terrain is only sampled directly below the bird (no
     * forward look-ahead / obstacle avoidance yet), so this keeps birds cruising at a believable height without
     * porting the full glide/circle/boids flight model.
     */
    private double verticalVelocity(World world, BirdSpecies.BirdSpeciesView view) {
        double groundY = world.getHeightValue((int) Math.floor(pos.x), (int) Math.floor(pos.z));

        double minAbove = view.minAltitudeAboveGround();
        double maxAbove = view.maxAltitudeAboveGround();
        double preferredAbove = clamp(view.preferredAboveGround(), minAbove, maxAbove);

        double floorY = groundY + minAbove;
        double ceilingY = groundY + maxAbove;
        double targetY = groundY + preferredAbove;

        // Only bias toward the floor/ceiling once we're close to violating it; otherwise cruise at the preferred band.
        if (pos.y < floorY + 6.0) {
            targetY = Math.max(targetY, floorY + 6.0);
        } else if (pos.y > ceilingY - 6.0) {
            targetY = Math.min(targetY, ceilingY - 6.0);
        }

        double yError = targetY - pos.y;
        double vy = clamp(yError * view.verticalAdjustStrength(), -0.06, 0.06);

        // Hard floor: never let the smoothed climb be too slow to clear the minimum altitude.
        if (pos.y + vy < floorY) {
            vy = Math.min(0.12, floorY - pos.y);
        }

        return vy;
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
            // First check for this call type: stagger the initial call instead of firing on spawn.
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
