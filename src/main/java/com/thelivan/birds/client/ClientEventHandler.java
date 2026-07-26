package com.thelivan.birds.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import com.thelivan.birds.client.render.RenderBird;
import com.thelivan.birds.client.sound.BirdSoundSystem;
import com.thelivan.birds.util.Vec3d;
import com.thelivan.birds.util.WeightedRandom;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ClientEventHandler {

    static final ClientEventHandler INSTANCE = new ClientEventHandler();
    static final Minecraft MC = Minecraft.getMinecraft();

    // Above the largest single flock (swallow big flock: up to 55) so a chosen flock doesn't get cut short.
    private static final int TARGET_BIRD_COUNT = 48;

    // A cell only exists to make spawn *decisions* (species/flock/size) deterministic and stable for a while;
    // birds themselves always spawn near the player's current view edge, same as before.
    private static final int SPAWN_CELL_SIZE = 128;
    private static final int SPAWN_RADIUS_CELLS = 2;
    private static final int SPAWN_TIME_WINDOW_TICKS = 20 * 30; // reroll each cell's decision every ~30s

    private static final double SPAWN_BORDER_BUFFER = 16.0;
    private static final double DESPAWN_BORDER_BUFFER = 32.0;

    private final Map<Long, ClientBird> birds = new HashMap<>();
    private final Map<Long, Flock> flocks = new HashMap<>();
    private boolean wasInWorld = false;

    private ClientEventHandler() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;

        World world = MC.theWorld;
        EntityPlayer player = MC.thePlayer;

        if (world == null || player == null) {
            birds.clear();
            flocks.clear();
            if (wasInWorld) BirdSoundSystem.stopAll();
            wasInWorld = false;
            return;
        }
        wasInWorld = true;

        for (Flock flock : flocks.values()) flock.tick();

        double despawnDist = MC.gameSettings.renderDistanceChunks * 16.0 + DESPAWN_BORDER_BUFFER;
        double despawnDist2 = despawnDist * despawnDist;

        Iterator<Map.Entry<Long, ClientBird>> it = birds.entrySet().iterator();
        while (it.hasNext()) {
            ClientBird bird = it.next()
                .getValue();

            Vec3d flockForward = (bird.flockId != 0L && flocks.containsKey(bird.flockId))
                ? flocks.get(bird.flockId)
                    .getGroupForward()
                : null;

            bird.tick(world, flockForward, birds.values());

            if (bird.ageTicks > 60 && bird.pos.squareDistanceTo(player.posX, player.posY, player.posZ) > despawnDist2) {
                BirdSoundSystem.stopForBird(bird.getId());
                it.remove();
            }
        }

        cleanupEmptyFlocks();

        if (birds.size() < TARGET_BIRD_COUNT) {
            spawnAroundPlayer(world, player);
        }

        BirdSoundSystem.purgeFinished();
    }

    private void spawnAroundPlayer(World world, EntityPlayer player) {
        long worldSeed = world.getSeed();
        int dim = world.provider.dimensionId;
        long window = world.getTotalWorldTime() / SPAWN_TIME_WINDOW_TICKS;
        boolean isDay = world.isDaytime();

        int cellX = Math.floorDiv((int) Math.floor(player.posX), SPAWN_CELL_SIZE);
        int cellZ = Math.floorDiv((int) Math.floor(player.posZ), SPAWN_CELL_SIZE);

        for (int dx = -SPAWN_RADIUS_CELLS; dx <= SPAWN_RADIUS_CELLS; dx++) {
            for (int dz = -SPAWN_RADIUS_CELLS; dz <= SPAWN_RADIUS_CELLS; dz++) {
                if (birds.size() >= TARGET_BIRD_COUNT) return;
                spawnCell(world, player, mixSeed(worldSeed, dim, cellX + dx, cellZ + dz, window), isDay);
            }
        }
    }

    private void spawnCell(World world, EntityPlayer player, long cellSeed, boolean isDay) {
        Random cellRandom = new Random(cellSeed);

        BirdSpecies species = pickSpecies(cellRandom, isDay);
        if (species == null) return;

        if (cellRandom.nextDouble() < species.flockChancePerCell) {
            spawnFlockForCell(world, player, cellSeed, cellRandom, species, isDay);
        } else {
            int count = 1 + cellRandom.nextInt(Math.max(1, species.birdsPerCellMax));
            for (int i = 0; i < count; i++) {
                long birdId = mixSeed(cellSeed, i, 1, 0, 0);
                if (birds.containsKey(birdId)) continue;
                if (birds.size() >= TARGET_BIRD_COUNT) return;

                birds.put(birdId, spawnSoloBird(world, player, cellRandom, species, birdId));
            }
        }
    }

    private ClientBird spawnSoloBird(World world, EntityPlayer player, Random random, BirdSpecies species, long birdId) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double spawnDist = viewBorder() + SPAWN_BORDER_BUFFER + random.nextDouble() * 64.0;

        int x = (int) Math.floor(player.posX + Math.cos(angle) * spawnDist);
        int z = (int) Math.floor(player.posZ + Math.sin(angle) * spawnDist);

        double groundY = world.getHeightValue(x, z);
        double above = clamp(species.preferredAboveGround, species.minAltitudeAboveGround, species.maxAltitudeAboveGround);
        Vec3d pos = new Vec3d(x, groundY + above, z);

        Vec3d dir = inwardDirection(angle, random);
        double speed = species.minSpeed + random.nextDouble() * (species.maxSpeed - species.minSpeed);

        return new ClientBird(species, birdId, pos, dir, speed);
    }

    private void spawnFlockForCell(World world, EntityPlayer player, long cellSeed, Random cellRandom, BirdSpecies species,
        boolean isDay) {
        double angle = cellRandom.nextDouble() * Math.PI * 2.0;
        double spawnDist = viewBorder() + SPAWN_BORDER_BUFFER + cellRandom.nextDouble() * 64.0;

        double sx = player.posX + Math.cos(angle) * spawnDist;
        double sz = player.posZ + Math.sin(angle) * spawnDist;
        double groundY = world.getHeightValue((int) Math.floor(sx), (int) Math.floor(sz));
        double above = clamp(species.preferredAboveGround, species.minAltitudeAboveGround, species.maxAltitudeAboveGround);
        Vec3d center = new Vec3d(sx, groundY + above, sz);

        Vec3d baseDir = inwardDirection(angle, cellRandom);
        double baseSpeed = species.minSpeed + cellRandom.nextDouble() * (species.maxSpeed - species.minSpeed);

        long flockId = mixSeed(cellSeed, 999, 7, 0, 0);
        if (flockId == 0L) flockId = 1L; // 0 is reserved to mean "not in a flock"

        int size = chooseFlockSize(cellRandom, isDay, species);

        // Not capped by TARGET_BIRD_COUNT: letting a flock finish in full matters more than the exact count.
        for (int i = 0; i < size; i++) {
            long birdId = mixSeed(cellSeed, i, 2, 0, 0);
            if (birds.containsKey(birdId)) continue;

            double spread = (size <= 10) ? (3.0 + cellRandom.nextDouble() * 8.0) : (6.0 + cellRandom.nextDouble() * 18.0);
            double a = cellRandom.nextDouble() * Math.PI * 2.0;
            Vec3d offset = new Vec3d(Math.cos(a) * spread, (cellRandom.nextDouble() - 0.5) * 3.0, Math.sin(a) * spread);
            Vec3d pos = center.add(offset);

            Vec3d jitter = new Vec3d((cellRandom.nextDouble() - 0.5) * 0.15, 0, (cellRandom.nextDouble() - 0.5) * 0.15);
            Vec3d dir = baseDir.add(jitter)
                .normalize();
            double speed = baseSpeed * (0.9 + cellRandom.nextDouble() * 0.2);

            ClientBird bird = new ClientBird(species, birdId, pos, dir, speed);
            bird.flockId = flockId;
            birds.put(birdId, bird);

            flocks.computeIfAbsent(flockId, id -> new Flock(id, baseDir));
        }
    }

    private static double viewBorder() {
        return MC.gameSettings.renderDistanceChunks * 16.0;
    }

    /** Spawn point is outward from the player at {@code angle}; heading points back inward, with some side wobble. */
    private static Vec3d inwardDirection(double angle, Random random) {
        Vec3d outward = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
        Vec3d side = new Vec3d(-outward.z, 0, outward.x);
        double sideAmt = (random.nextDouble() - 0.5) * 1.2;
        return outward.scale(-1.0)
            .add(side.scale(sideAmt))
            .normalize();
    }

    private int chooseFlockSize(Random random, boolean isDay, BirdSpecies species) {
        double bigChance = isDay ? species.bigFlockChanceDay : species.bigFlockChanceNight;

        int min, max;
        if (random.nextDouble() < bigChance) {
            min = Math.min(species.bigFlockMin, species.bigFlockMax);
            max = Math.max(species.bigFlockMin, species.bigFlockMax);
        } else {
            min = Math.min(species.flockMin, species.flockMax);
            max = Math.max(species.flockMin, species.flockMax);
        }

        if (max <= 0) return 1;
        return Math.max(1, min + random.nextInt(max - min + 1));
    }

    private void cleanupEmptyFlocks() {
        if (flocks.isEmpty()) return;

        Set<Long> used = new HashSet<>();
        for (ClientBird bird : birds.values()) {
            if (bird.flockId != 0L) used.add(bird.flockId);
        }

        flocks.keySet()
            .removeIf(id -> !used.contains(id));
    }

    private BirdSpecies pickSpecies(Random random, boolean isDay) {
        List<BirdSpecies> allowed = new ArrayList<>();
        for (BirdSpecies s : BirdSpeciesRegistry.ALL) {
            if (s.spawnWeight <= 0) continue;
            if (isDay && !s.canSpawnAtDay) continue;
            if (!isDay && !s.canSpawnAtNight) continue;

            allowed.add(s);
        }

        return WeightedRandom.pick(allowed, s -> s.spawnWeight, random);
    }

    /** Deterministic hash of up to 5 longs into a single seed (murmur3-style finalizer over XOR-folded inputs). */
    private static long mixSeed(long a, long b, long c, long d, long e) {
        long x = a;
        x ^= b * 0x9E3779B97F4A7C15L;
        x ^= c * 0xC2B2AE3D27D4EB4FL;
        x ^= d * 0x165667B19E3779F9L;
        x ^= e * 0x85EBCA6BL;
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        return x;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        if (MC.theWorld == null || MC.thePlayer == null) return;

        RenderBird.renderAll(birds.values(), ev.partialTicks);
    }
}
