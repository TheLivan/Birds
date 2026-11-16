package com.thelivan.birds.client.sound;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

import com.thelivan.birds.Birds;
import com.thelivan.birds.client.BirdSpecies;
import com.thelivan.birds.client.ClientBird;

/**
 * Dispatches and tracks currently-playing bird calls; vanilla's sound engine already does weighted-random pool
 * selection over the {@code assets/birds/sounds.json} events, so this only decides *when* a bird may call and keeps
 * a single active sound per bird.
 */
public final class BirdSoundSystem {

    private static final int MAX_STARTS_PER_TICK = 4;
    // The sound engine starts streams asynchronously; allow a few ticks before treating "not playing" as stale.
    private static final int STARTUP_GRACE_TICKS = 5;

    private static final Map<Long, BirdCallSound> ACTIVE = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private static int lastTick = -1;
    private static int startedThisTick = 0;

    private BirdSoundSystem() {}

    public static void playCall(ClientBird bird, BirdCallType type, BirdSpecies.SoundView sound) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;

        SoundHandler sh = mc.getSoundHandler();
        BirdSpecies species = bird.species;

        long birdId = bird.getId();
        int tick = (int) mc.theWorld.getTotalWorldTime();

        // A one-shot sound disappears from the engine when the ogg ends, while isDonePlaying() only flips for the
        // out-of-range case. Check the engine itself, otherwise a bird stays stuck on its first (already finished)
        // call forever.
        BirdCallSound existing = ACTIVE.get(birdId);
        if (existing != null) {
            if (sh.isSoundPlaying(existing)) return;

            if (tick - existing.getStartTick() < STARTUP_GRACE_TICKS && !existing.isDonePlaying()) return;

            ACTIVE.remove(birdId);
            sh.stopSound(existing); // no-op for a naturally finished sound, frees one stuck while starting
        }

        double maxDist = sound.soundMaxDistance();
        if (bird.pos.squareDistanceTo(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ) > maxDist * maxDist) {
            return;
        }

        if (tick != lastTick) {
            lastTick = tick;
            startedThisTick = 0;
        }
        if (startedThisTick >= MAX_STARTS_PER_TICK) return;

        ResourceLocation event = new ResourceLocation(Birds.MODID, species.folderName + "." + type.subdir);

        float pitchJitter = 1.0f + (float) ((RANDOM.nextDouble() * 2.0 - 1.0) * sound.soundPitchVariation());
        float pitch = (float) sound.soundPitch() * pitchJitter;
        float volume = (float) sound.soundVolume();

        BirdCallSound call = new BirdCallSound(
            bird,
            event,
            volume,
            pitch,
            (float) sound.soundMaxDistance(),
            (float) sound.soundFadeStart(),
            (float) sound.soundFadePower(),
            tick);

        ACTIVE.put(birdId, call);
        startedThisTick++;

        sh.playSound(call);
    }

    public static void stopForBird(long birdId) {
        BirdCallSound call = ACTIVE.remove(birdId);
        if (call == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.theWorld != null) {
            mc.getSoundHandler()
                .stopSound(call);
        }
    }

    public static void stopAll() {
        Minecraft mc = Minecraft.getMinecraft();
        SoundHandler sh = (mc != null) ? mc.getSoundHandler() : null;

        if (sh != null) {
            for (BirdCallSound call : ACTIVE.values()) {
                sh.stopSound(call);
            }
        }

        ACTIVE.clear();
    }

    /**
     * Drops finished/stale entries so a bird that stops calling doesn't keep a dead {@link BirdCallSound} pinned in the
     * map forever, and so a finished call never blocks the bird's next one.
     */
    public static void purgeFinished() {
        Minecraft mc = Minecraft.getMinecraft();
        SoundHandler sh = (mc != null) ? mc.getSoundHandler() : null;

        if (sh == null || mc.theWorld == null) {
            ACTIVE.values()
                .removeIf(BirdCallSound::isDonePlaying);
            return;
        }

        int tick = (int) mc.theWorld.getTotalWorldTime();
        ACTIVE.entrySet()
            .removeIf(entry -> {
                BirdCallSound call = entry.getValue();
                if (call.isDonePlaying()) return true;
                if (sh.isSoundPlaying(call)) return false;
                return tick - call.getStartTick() >= STARTUP_GRACE_TICKS;
            });
    }
}
