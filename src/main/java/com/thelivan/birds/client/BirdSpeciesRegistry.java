package com.thelivan.birds.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import com.thelivan.birds.Birds;

/**
 * Hardcoded bird species. There is no JSON/config loader for species anymore: adding a new one means adding a
 * factory method here and dropping its {@code single}/{@code flock} .ogg files under
 * {@code assets/birds/sounds/<folderName>/} (plus matching entries in {@code assets/birds/sounds.json}).
 */
public final class BirdSpeciesRegistry {

    public static final BirdSpecies GOLDEN_EAGLE = goldenEagle();
    public static final BirdSpecies HARRIER = harrier();
    public static final BirdSpecies SWALLOW = swallow();

    public static final List<BirdSpecies> ALL = Collections
        .unmodifiableList(Arrays.asList(GOLDEN_EAGLE, HARRIER, SWALLOW));

    private BirdSpeciesRegistry() {}

    private static BirdSpecies goldenEagle() {
        BirdSpecies s = base("Golden eagle", "golden_eagle");

        s.spawnWeight = 0.4;
        s.minSpeed = 0.4;
        s.maxSpeed = 0.82;
        s.maxTurnDegPerTick = 4.8;
        s.scale = 1.2;
        s.flapAmplitude = 0.045;
        s.flapSpeed = 0.22;

        s.soundSingle = soundSettings(1.0, 0.6, 1200, 0.25, 128.0, 8.0, 2.0, 0.05);
        s.soundFlock = soundSettings(1.0, 0.6, 600, 0.5, 128.0, 8.0, 2.0, 0.05);

        s.biomeRules.temperatureCategoryWhitelist = Arrays.asList("COLD", "MEDIUM");
        s.biomeRules.biomeDictionaryWhitelist = Arrays.asList("MOUNTAIN", "HILLS", "COLD");
        s.biomeRules.biomeDictionaryBlacklist = Arrays.asList("NETHER", "END", "OCEAN", "HOT");

        return finish(s);
    }

    private static BirdSpecies harrier() {
        BirdSpecies s = base("Northern harrier", "harrier");

        s.spawnWeight = 0.45;
        s.minSpeed = 0.34;
        s.maxSpeed = 0.64;
        s.maxTurnDegPerTick = 5.6;
        s.scale = 0.7;
        s.flapAmplitude = 0.055;
        s.flapSpeed = 0.26;

        // The 1.12.2 source shipped .ogg calls for this species but never defined sound settings for it;
        // these are new, tuned to sit between the eagle and the swallow.
        s.soundSingle = soundSettings(1.0, 0.5, 500, 0.3, 100.0, 6.0, 1.8, 0.06);
        s.soundFlock = soundSettings(1.0, 0.5, 260, 0.4, 100.0, 6.0, 1.8, 0.06);

        s.biomeRules.biomeDictionaryWhitelist = Arrays.asList("SWAMP", "WET", "PLAINS", "RIVER");
        s.biomeRules.biomeDictionaryBlacklist = Arrays.asList("NETHER", "END", "OCEAN", "MOUNTAIN");

        return finish(s);
    }

    private static BirdSpecies swallow() {
        BirdSpecies s = base("Swallow", "swallow");

        s.spawnWeight = 0.6;
        s.minSpeed = 0.5;
        s.maxSpeed = 0.78;
        s.maxTurnDegPerTick = 5.5;
        s.scale = 0.2;
        s.flapAmplitude = 0.2;
        s.flapSpeed = 0.48;

        s.soundSingle = soundSettings(1.0, 0.3, 600, 0.25, 128.0, 2.0, 2.5, 0.07);
        s.soundFlock = soundSettings(1.0, 0.3, 420, 0.0, 128.0, 2.0, 2.5, 0.06);

        s.biomeRules.biomeDictionaryWhitelist = Arrays.asList("PLAINS", "RIVER", "FOREST", "SWAMP");
        s.biomeRules.biomeDictionaryBlacklist = Arrays.asList("NETHER", "END", "OCEAN");

        return finish(s);
    }

    private static BirdSpecies base(String name, String folderName) {
        BirdSpecies s = new BirdSpecies();
        s.name = name;
        s.folderName = folderName;
        s.soundKey = folderName;

        for (int i = 1; i <= 5; i++) {
            s.textures.add(new ResourceLocation(Birds.MODID, "textures/bird_" + i + ".png"));
        }

        return s;
    }

    private static BirdSpecies.SoundSettings soundSettings(double pitch, double volume, int intervalTicks,
        double randomness, double maxDistance, double fadeStart, double fadePower, double pitchVariation) {
        BirdSpecies.SoundSettings ss = new BirdSpecies.SoundSettings();
        ss.soundPitch = pitch;
        ss.soundVolume = volume;
        ss.soundBaseIntervalTicks = intervalTicks;
        ss.soundRandomness = randomness;
        ss.soundMaxDistance = maxDistance;
        ss.soundFadeStart = fadeStart;
        ss.soundFadePower = fadePower;
        ss.soundPitchVariation = pitchVariation;
        return ss;
    }

    private static BirdSpecies finish(BirdSpecies s) {
        BiomeRuleResolver.resolve(s);
        s.clampAndFix();
        return s;
    }
}
