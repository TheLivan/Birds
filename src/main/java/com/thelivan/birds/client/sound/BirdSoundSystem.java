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
 * Dispatches and tracks the currently-playing bird call sounds.
 * <p>
 * Species sounds are declared once in {@code assets/birds/sounds.json} as ordinary events (one per
 * {@code <folderName>.<single|flock>}); vanilla's own sound engine already does weighted-random pool selection for
 * us, so this class only has to decide *when* a bird is allowed to start a call and keep a single active sound per
 * bird so a fast-repeating species can't stack calls on top of itself.
 */
public final class BirdSoundSystem {

    private static final int MAX_STARTS_PER_TICK = 4;

    private static final Map<Long, BirdCallSound> ACTIVE = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();

    private static int lastTick = -1;
    private static int startedThisTick = 0;

    private BirdSoundSystem() {}

    public static void playCall(ClientBird bird, BirdCallType type, BirdSpecies.SoundView sound) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;

        BirdSpecies species = bird.species;
        if (species == null || species.folderName == null) return;

        long birdId = bird.getId();
        BirdCallSound existing = ACTIVE.get(birdId);
        if (existing != null && !existing.isDonePlaying()) return;

        double maxDist = sound.soundMaxDistance();
        if (bird.pos.squareDistanceTo(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ) > maxDist * maxDist) {
            return;
        }

        int tick = (int) mc.theWorld.getTotalWorldTime();
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
            (float) sound.soundFadePower());

        ACTIVE.put(birdId, call);
        startedThisTick++;

        mc.getSoundHandler()
            .playSound(call);
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
     * Drops finished entries so a bird that stops calling (interval elapsed once, never re-triggered) doesn't keep a
     * dead {@link BirdCallSound} pinned in the map forever.
     */
    public static void purgeFinished() {
        ACTIVE.values()
            .removeIf(BirdCallSound::isDonePlaying);
    }
}
