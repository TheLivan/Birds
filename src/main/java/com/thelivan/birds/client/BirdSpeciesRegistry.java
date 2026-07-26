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
    public static final BirdSpecies COMMON_SWIFT = commonSwift();
    public static final BirdSpecies EURASIAN_GRIFFON = eurasianGriffon();

    public static final List<BirdSpecies> ALL = Collections
        .unmodifiableList(Arrays.asList(GOLDEN_EAGLE, HARRIER, SWALLOW, COMMON_SWIFT, EURASIAN_GRIFFON));

    private BirdSpeciesRegistry() {}

    private static BirdSpecies goldenEagle() {
        BirdSpecies s = base("Golden eagle", "golden_eagle");

        s.spawnWeight = 0.4;
        s.minSpeed = 0.4;
        s.maxSpeed = 0.82;
        s.maxTurnDegPerTick = 4.8;
        s.noiseStrength = 0.02;
        s.minAltitudeAboveGround = 45.0;
        s.maxAltitudeAboveGround = 64.0;
        s.preferredAboveGround = 48.0;
        s.verticalAdjustStrength = 0.0036;
        s.scale = 1.2;
        s.flapAmplitude = 0.045;
        s.flapSpeed = 0.22;

        // Long, wide glides; circles less often and more rarely than harrier/swallow.
        s.glideMinTicks = 110;
        s.glideMaxTicks = 260;
        s.circleMinTicks = 120;
        s.circleMaxTicks = 320;
        s.circleRadiusMin = 28.0;
        s.circleRadiusMax = 120.0;
        s.patternWeightGlide = 0.7;
        s.patternWeightCircle = 0.3;

        // Eagles are near-solitary: rarely a pair, essentially never a big flock.
        s.birdsPerCellMax = 2;
        s.flockChancePerCell = 0.07;
        s.flockMin = 1;
        s.flockMax = 2;
        s.bigFlockChanceDay = 0.0;
        s.bigFlockChanceNight = 0.05;
        s.bigFlockMin = 0;
        s.bigFlockMax = 0;

        s.soundSingle = soundSettings(1.0, 0.6, 1200, 0.25, 128.0, 8.0, 2.0, 0.05);
        s.soundFlock = soundSettings(1.0, 0.6, 600, 0.5, 128.0, 8.0, 2.0, 0.05);

        return finish(s);
    }

    private static BirdSpecies harrier() {
        BirdSpecies s = base("Northern harrier", "harrier");

        s.spawnWeight = 0.45;
        s.minSpeed = 0.34;
        s.maxSpeed = 0.64;
        s.maxTurnDegPerTick = 5.6;
        s.noiseStrength = 0.03;
        s.minAltitudeAboveGround = 8.0;
        s.maxAltitudeAboveGround = 64.0;
        s.preferredAboveGround = 48.0;
        s.verticalAdjustStrength = 0.0048;
        s.scale = 0.7;
        s.flapAmplitude = 0.055;
        s.flapSpeed = 0.26;

        s.glideMinTicks = 70;
        s.glideMaxTicks = 180;
        s.circleMinTicks = 80;
        s.circleMaxTicks = 220;
        s.circleRadiusMin = 18.0;
        s.circleRadiusMax = 85.0;
        s.patternWeightGlide = 0.58;
        s.patternWeightCircle = 0.42;

        s.birdsPerCellMax = 3;
        s.flockChancePerCell = 0.1;
        s.flockMin = 1;
        s.flockMax = 2;
        s.bigFlockChanceDay = 0.0;
        s.bigFlockChanceNight = 0.05;
        s.bigFlockMin = 0;
        s.bigFlockMax = 0;

        // The 1.12.2 source shipped .ogg calls for this species but never defined sound settings for it;
        // these are new, tuned to sit between the eagle and the swallow.
        s.soundSingle = soundSettings(1.0, 0.5, 500, 0.3, 100.0, 6.0, 1.8, 0.06);
        s.soundFlock = soundSettings(1.0, 0.5, 260, 0.4, 100.0, 6.0, 1.8, 0.06);

        return finish(s);
    }

    private static BirdSpecies swallow() {
        BirdSpecies s = base("Swallow", "swallow");

        s.spawnWeight = 0.6;
        s.minSpeed = 0.5;
        s.maxSpeed = 0.78;
        s.maxTurnDegPerTick = 5.5;
        s.noiseStrength = 0.055;
        s.minAltitudeAboveGround = 8.0;
        s.maxAltitudeAboveGround = 32.0;
        s.preferredAboveGround = 12.0;
        s.verticalAdjustStrength = 0.006;
        s.scale = 0.2;
        s.flapAmplitude = 0.2;
        s.flapSpeed = 0.48;

        s.glideMinTicks = 45;
        s.glideMaxTicks = 110;
        s.circleMinTicks = 60;
        s.circleMaxTicks = 170;
        s.circleRadiusMin = 10.0;
        s.circleRadiusMax = 40.0;
        s.patternWeightGlide = 0.5;
        s.patternWeightCircle = 0.5;

        // Swallows are the flocking species: most spawns are a group, sometimes a big one.
        s.birdsPerCellMax = 16;
        s.flockChancePerCell = 0.6;
        s.flockMin = 8;
        s.flockMax = 22;
        s.bigFlockChanceDay = 0.3;
        s.bigFlockChanceNight = 0.2;
        s.bigFlockMin = 25;
        s.bigFlockMax = 55;

        s.soundSingle = soundSettings(1.0, 0.3, 600, 0.25, 128.0, 2.0, 2.5, 0.07);
        s.soundFlock = soundSettings(1.0, 0.3, 420, 0.0, 128.0, 2.0, 2.5, 0.06);

        return finish(s);
    }

    private static BirdSpecies commonSwift() {
        BirdSpecies s = base("Common swift", "common_swift");

        s.spawnWeight = 0.5;
        s.minSpeed = 0.5;
        s.maxSpeed = 0.92;
        s.maxTurnDegPerTick = 7.8;
        s.noiseStrength = 0.0;
        s.minAltitudeAboveGround = 28.0;
        s.maxAltitudeAboveGround = 140.0;
        s.preferredAboveGround = 12.0;
        s.verticalAdjustStrength = 0.005;
        s.scale = 0.3;
        s.flapAmplitude = 0.07;
        s.flapSpeed = 0.6;

        // Swifts glide in very long, uninterrupted stretches (minutes, not seconds) compared to the other species.
        s.glideMinTicks = 2000;
        s.glideMaxTicks = 3000;
        s.circleMinTicks = 60;
        s.circleMaxTicks = 160;
        s.circleRadiusMin = 14.0;
        s.circleRadiusMax = 55.0;
        s.patternWeightGlide = 0.55;
        s.patternWeightCircle = 0.45;

        s.birdsPerCellMax = 7;
        s.flockChancePerCell = 0.55;
        s.flockMin = 4;
        s.flockMax = 12;
        s.bigFlockChanceDay = 0.15;
        s.bigFlockChanceNight = 0.05;
        s.bigFlockMin = 12;
        s.bigFlockMax = 26;

        s.soundSingle = soundSettings(1.0, 0.3, 120, 0.25, 96.0, 8.0, 1.8, 0.05);
        s.soundFlock = soundSettings(1.0, 0.3, 420, 0.5, 96.0, 8.0, 1.8, 0.05);

        return finish(s);
    }

    private static BirdSpecies eurasianGriffon() {
        BirdSpecies s = base("Eurasian griffon", "eurasiangriffon");

        s.spawnWeight = 0.22;
        s.minSpeed = 0.3;
        s.maxSpeed = 0.6;
        s.maxTurnDegPerTick = 3.8;
        s.noiseStrength = 0.018;
        s.minAltitudeAboveGround = 50.0;
        s.maxAltitudeAboveGround = 290.0;
        s.preferredAboveGround = 48.0;
        s.verticalAdjustStrength = 0.0032;
        s.scale = 1.37;
        s.flapAmplitude = 0.032;
        s.flapSpeed = 0.19;

        s.glideMinTicks = 130;
        s.glideMaxTicks = 310;
        s.circleMinTicks = 150;
        s.circleMaxTicks = 360;
        s.circleRadiusMin = 36.0;
        s.circleRadiusMax = 150.0;
        s.patternWeightGlide = 0.76;
        s.patternWeightCircle = 0.24;

        s.birdsPerCellMax = 3;
        s.flockChancePerCell = 0.28;
        s.flockMin = 2;
        s.flockMax = 6;
        s.bigFlockChanceDay = 0.08;
        s.bigFlockChanceNight = 0.05;
        s.bigFlockMin = 7;
        s.bigFlockMax = 16;

        // No .ogg calls exist for this species in the 1.12.2 source either — silent there too.
        s.soundsEnabled = false;

        return finish(s);
    }

    private static BirdSpecies base(String name, String folderName) {
        BirdSpecies s = new BirdSpecies();
        s.name = name;
        s.folderName = folderName;
        s.soundKey = folderName;
        s.textures.add(new ResourceLocation(Birds.MODID, "textures/" + folderName + ".png"));

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
        s.clampAndFix();
        return s;
    }
}
