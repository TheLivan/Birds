package com.thelivan.birds.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.thelivan.birds.client.BirdSpecies.SoundSettings;

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
        return BirdSpecies.builder("Golden eagle", "golden_eagle")
            .spawnWeight(0.4)
            .flight(0.4, 0.82, 4.8, 0.02)
            .altitude(45.0, 64.0, 48.0, 0.0036)
            .render(1.2, 0.045, 0.22)
            // Long, wide glides; circles less often and more rarely than harrier/swallow.
            .pattern(110, 260, 120, 320, 28.0, 120.0, 0.7, 0.3)
            // Eagles are near-solitary: rarely a pair, essentially never a big flock.
            .flock(2, 0.07, 1, 2, 0.0, 0.05, 0, 0)
            .sound(
                SoundSettings.of(1.0, 0.6, 1200, 0.25, 128.0, 8.0, 2.0, 0.05),
                SoundSettings.of(1.0, 0.6, 600, 0.5, 128.0, 8.0, 2.0, 0.05))
            .build();
    }

    private static BirdSpecies harrier() {
        return BirdSpecies.builder("Northern harrier", "harrier")
            .spawnWeight(0.45)
            .flight(0.34, 0.64, 5.6, 0.03)
            .altitude(8.0, 64.0, 48.0, 0.0048)
            .render(0.7, 0.055, 0.26)
            .pattern(70, 180, 80, 220, 18.0, 85.0, 0.58, 0.42)
            .flock(3, 0.1, 1, 2, 0.0, 0.05, 0, 0)
            // The 1.12.2 source shipped .ogg calls for this species but never defined sound settings for it;
            // these are new, tuned to sit between the eagle and the swallow.
            .sound(
                SoundSettings.of(1.0, 0.5, 500, 0.3, 100.0, 6.0, 1.8, 0.06),
                SoundSettings.of(1.0, 0.5, 260, 0.4, 100.0, 6.0, 1.8, 0.06))
            .build();
    }

    private static BirdSpecies swallow() {
        return BirdSpecies.builder("Swallow", "swallow")
            .spawnWeight(0.6)
            .flight(0.5, 0.78, 5.5, 0.055)
            .altitude(8.0, 32.0, 12.0, 0.006)
            .render(0.2, 0.2, 0.48)
            .pattern(45, 110, 60, 170, 10.0, 40.0, 0.5, 0.5)
            // Swallows are the flocking species: most spawns are a group, sometimes a big one.
            .flock(16, 0.6, 8, 22, 0.3, 0.2, 25, 55)
            .sound(
                SoundSettings.of(1.0, 0.3, 600, 0.25, 128.0, 2.0, 2.5, 0.07),
                SoundSettings.of(1.0, 0.3, 420, 0.0, 128.0, 2.0, 2.5, 0.06))
            .build();
    }

    private static BirdSpecies commonSwift() {
        return BirdSpecies.builder("Common swift", "common_swift")
            .spawnWeight(0.5)
            .flight(0.5, 0.92, 7.8, 0.0)
            .altitude(28.0, 140.0, 12.0, 0.005)
            .render(0.3, 0.07, 0.6)
            // Swifts glide in very long, uninterrupted stretches (minutes, not seconds) compared to the other
            // species.
            .pattern(2000, 3000, 60, 160, 14.0, 55.0, 0.55, 0.45)
            .flock(7, 0.55, 4, 12, 0.15, 0.05, 12, 26)
            .sound(
                SoundSettings.of(1.0, 0.3, 120, 0.25, 96.0, 8.0, 1.8, 0.05),
                SoundSettings.of(1.0, 0.3, 420, 0.5, 96.0, 8.0, 1.8, 0.05))
            .build();
    }

    private static BirdSpecies eurasianGriffon() {
        return BirdSpecies.builder("Eurasian griffon", "eurasiangriffon")
            .spawnWeight(0.22)
            .flight(0.3, 0.6, 3.8, 0.018)
            .altitude(50.0, 290.0, 48.0, 0.0032)
            .render(1.37, 0.032, 0.19)
            .pattern(130, 310, 150, 360, 36.0, 150.0, 0.76, 0.24)
            .flock(3, 0.28, 2, 6, 0.08, 0.05, 7, 16)
            // No .ogg calls exist for this species in the 1.12.2 source either — silent there too.
            .silent()
            .build();
    }
}
