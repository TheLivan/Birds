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

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * PLACEHOLDER driver that keeps a handful of birds alive around the player so the renderer can be verified.
 * It is replaced by the ported BirdManager + FlockSpawner once the full spawning logic (per-cell density, biomes)
 * lands.
 */
public class ClientEventHandler {

    static final ClientEventHandler INSTANCE = new ClientEventHandler();
    static final Minecraft MC = Minecraft.getMinecraft();
    static final Random rnd = new Random();

    private static final int TARGET_BIRD_COUNT = 20;
    private static final int SPAWN_RADIUS = 80;
    private static final double DESPAWN_BORDER_BUFFER = 32.0;

    private final List<ClientBird> birds = new ArrayList<>();
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

        Iterator<ClientBird> it = birds.iterator();
        while (it.hasNext()) {
            ClientBird bird = it.next();

            Vec3d flockForward = (bird.flockId != 0L && flocks.containsKey(bird.flockId))
                ? flocks.get(bird.flockId)
                    .getGroupForward()
                : null;

            bird.tick(world, flockForward, birds);

            if (bird.ageTicks > 60 && bird.pos.squareDistanceTo(player.posX, player.posY, player.posZ) > despawnDist2) {
                BirdSoundSystem.stopForBird(bird.getId());
                it.remove();
            }
        }

        cleanupEmptyFlocks();

        while (birds.size() < TARGET_BIRD_COUNT) {
            BirdSpecies species = pickSpecies(world);
            if (species == null) break; // nobody is allowed to spawn right now (e.g. every species is day-only, and it's night)

            if (rnd.nextDouble() < species.flockChancePerCell) {
                spawnFlock(world, player, species);
            } else {
                birds.add(spawnSoloBird(world, player, species));
            }
        }

        BirdSoundSystem.purgeFinished();
    }

    private ClientBird spawnSoloBird(World world, EntityPlayer player, BirdSpecies species) {
        int x = (int) player.posX + rnd.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
        int z = (int) player.posZ + rnd.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;

        double groundY = world.getHeightValue(x, z);
        double above = clamp(species.preferredAboveGround, species.minAltitudeAboveGround, species.maxAltitudeAboveGround);
        double y = groundY + above;

        double angle = rnd.nextDouble() * Math.PI * 2.0;
        Vec3d dir = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));

        double speed = species.minSpeed + rnd.nextDouble() * (species.maxSpeed - species.minSpeed);

        return new ClientBird(world, species, rnd.nextLong(), new Vec3d(x, y, z), dir, speed);
    }

    private void spawnFlock(World world, EntityPlayer player, BirdSpecies species) {
        int x = (int) player.posX + rnd.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
        int z = (int) player.posZ + rnd.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;

        double groundY = world.getHeightValue(x, z);
        double above = clamp(species.preferredAboveGround, species.minAltitudeAboveGround, species.maxAltitudeAboveGround);
        Vec3d center = new Vec3d(x, groundY + above, z);

        double angle = rnd.nextDouble() * Math.PI * 2.0;
        Vec3d baseDir = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
        double baseSpeed = species.minSpeed + rnd.nextDouble() * (species.maxSpeed - species.minSpeed);

        long flockId = rnd.nextLong();
        if (flockId == 0L) flockId = 1L; // 0 is reserved to mean "not in a flock"
        flocks.put(flockId, new Flock(flockId, baseDir));

        int size = chooseFlockSize(world, species);

        for (int i = 0; i < size && birds.size() < TARGET_BIRD_COUNT; i++) {
            double spread = (size <= 10) ? (3.0 + rnd.nextDouble() * 8.0) : (6.0 + rnd.nextDouble() * 18.0);
            double a = rnd.nextDouble() * Math.PI * 2.0;
            Vec3d offset = new Vec3d(Math.cos(a) * spread, (rnd.nextDouble() - 0.5) * 3.0, Math.sin(a) * spread);
            Vec3d pos = center.add(offset);

            Vec3d jitter = new Vec3d((rnd.nextDouble() - 0.5) * 0.15, 0, (rnd.nextDouble() - 0.5) * 0.15);
            Vec3d dir = baseDir.add(jitter)
                .normalize();
            double speed = baseSpeed * (0.9 + rnd.nextDouble() * 0.2);

            ClientBird bird = new ClientBird(world, species, rnd.nextLong(), pos, dir, speed);
            bird.flockId = flockId;
            birds.add(bird);
        }
    }

    private int chooseFlockSize(World world, BirdSpecies species) {
        boolean day = world.isDaytime();
        double bigChance = day ? species.bigFlockChanceDay : species.bigFlockChanceNight;

        int min, max;
        if (rnd.nextDouble() < bigChance) {
            min = Math.min(species.bigFlockMin, species.bigFlockMax);
            max = Math.max(species.bigFlockMin, species.bigFlockMax);
        } else {
            min = Math.min(species.flockMin, species.flockMax);
            max = Math.max(species.flockMin, species.flockMax);
        }

        if (max <= 0) return 1;
        return Math.max(1, min + rnd.nextInt(max - min + 1));
    }

    private void cleanupEmptyFlocks() {
        if (flocks.isEmpty()) return;

        Set<Long> used = new HashSet<>();
        for (ClientBird bird : birds) {
            if (bird.flockId != 0L) used.add(bird.flockId);
        }

        flocks.keySet()
            .removeIf(id -> !used.contains(id));
    }

    private BirdSpecies pickSpecies(World world) {
        boolean isDay = world.isDaytime();
        List<BirdSpecies> all = BirdSpeciesRegistry.ALL;

        List<BirdSpecies> allowed = new ArrayList<>();
        double totalWeight = 0.0;
        for (BirdSpecies s : all) {
            if (s.spawnWeight <= 0) continue;
            if (isDay && !s.canSpawnAtDay) continue;
            if (!isDay && !s.canSpawnAtNight) continue;

            allowed.add(s);
            totalWeight += s.spawnWeight;
        }
        if (allowed.isEmpty()) return null;

        double pick = rnd.nextDouble() * totalWeight;
        double acc = 0.0;
        for (BirdSpecies s : allowed) {
            acc += s.spawnWeight;
            if (pick < acc) return s;
        }

        return allowed.get(allowed.size() - 1);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        if (MC.theWorld == null || MC.thePlayer == null) return;

        RenderBird.renderAll(birds, ev.partialTicks);
    }
}
