package com.thelivan.birds.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
 * It is replaced by the ported BirdManager + FlockSpawner once the flight and spawning logic lands.
 */
public class ClientEventHandler {

    static final ClientEventHandler INSTANCE = new ClientEventHandler();
    static final Minecraft MC = Minecraft.getMinecraft();
    static final Random rnd = new Random();

    private static final int TARGET_BIRD_COUNT = 20;
    private static final int SPAWN_RADIUS = 80;
    private static final double DESPAWN_BORDER_BUFFER = 32.0;

    private final List<ClientBird> birds = new ArrayList<>();
    private boolean wasInWorld = false;

    private ClientEventHandler() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;

        World world = MC.theWorld;
        EntityPlayer player = MC.thePlayer;

        if (world == null || player == null) {
            birds.clear();
            if (wasInWorld) BirdSoundSystem.stopAll();
            wasInWorld = false;
            return;
        }
        wasInWorld = true;

        double despawnDist = MC.gameSettings.renderDistanceChunks * 16.0 + DESPAWN_BORDER_BUFFER;
        double despawnDist2 = despawnDist * despawnDist;

        Iterator<ClientBird> it = birds.iterator();
        while (it.hasNext()) {
            ClientBird bird = it.next();
            bird.tick(world);

            if (bird.ageTicks > 60 && bird.pos.squareDistanceTo(player.posX, player.posY, player.posZ) > despawnDist2) {
                BirdSoundSystem.stopForBird(bird.getId());
                it.remove();
            }
        }

        while (birds.size() < TARGET_BIRD_COUNT) {
            birds.add(spawnBird(world, player));
        }

        BirdSoundSystem.purgeFinished();
    }

    private ClientBird spawnBird(World world, EntityPlayer player) {
        int x = (int) player.posX + rnd.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;
        int z = (int) player.posZ + rnd.nextInt(SPAWN_RADIUS * 2) - SPAWN_RADIUS;

        BirdSpecies species = pickSpecies();

        double groundY = world.getHeightValue(x, z);
        double above = clamp(species.preferredAboveGround, species.minAltitudeAboveGround, species.maxAltitudeAboveGround);
        double y = groundY + above;

        double angle = rnd.nextDouble() * Math.PI * 2.0;
        Vec3d dir = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));

        double speed = species.minSpeed + rnd.nextDouble() * (species.maxSpeed - species.minSpeed);

        return new ClientBird(world, species, rnd.nextLong(), new Vec3d(x, y, z), dir, speed);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private BirdSpecies pickSpecies() {
        List<BirdSpecies> all = BirdSpeciesRegistry.ALL;

        double totalWeight = 0.0;
        for (BirdSpecies s : all) totalWeight += Math.max(0.0, s.spawnWeight);

        double pick = rnd.nextDouble() * totalWeight;
        double acc = 0.0;
        for (BirdSpecies s : all) {
            acc += Math.max(0.0, s.spawnWeight);
            if (pick < acc) return s;
        }

        return all.get(all.size() - 1);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        if (MC.theWorld == null || MC.thePlayer == null) return;

        RenderBird.renderAll(birds, ev.partialTicks);
    }
}
