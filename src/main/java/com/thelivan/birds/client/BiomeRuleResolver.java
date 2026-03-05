package com.thelivan.birds.client;

import java.util.Locale;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;

public class BiomeRuleResolver {

    public static void resolve(BirdSpecies species) {
        if (species == null || species.biomeRules == null) return;

        BirdSpecies.BiomeRules rules = species.biomeRules;

        rules.resolvedTempCats.clear();
        rules.resolvedBiomeManagerTypes.clear();
        rules.resolvedDictWhitelist.clear();
        rules.resolvedDictBlacklist.clear();

        // Temp categories
        if (rules.temperatureCategoryWhitelist != null) {
            for (String x : rules.temperatureCategoryWhitelist) {
                BiomeGenBase.TempCategory cat = parseTempCategory(x);
                if (cat != null) rules.resolvedTempCats.add(cat);
                else warn(species, "temperatureCategoryWhitelist", x);
            }
        }

        // BiomeManager types
        if (rules.biomeManagerTypeWhitelist != null) {
            for (String x : rules.biomeManagerTypeWhitelist) {
                BiomeManager.BiomeType t = parseBiomeManagerType(x);
                if (t != null) rules.resolvedBiomeManagerTypes.add(t);
                else warn(species, "biomeManagerTypeWhitelist", x);
            }
        }

        // BiomeDictionary whitelist
        if (rules.biomeDictionaryWhitelist != null) {
            for (String x : rules.biomeDictionaryWhitelist) {
                BiomeDictionary.Type t = parseBiomeDictionaryType(x);
                if (t != null) rules.resolvedDictWhitelist.add(t);
                else warn(species, "biomeDictionaryWhitelist", x);
            }
        }

        // BiomeDictionary blacklist
        if (rules.biomeDictionaryBlacklist != null) {
            for (String x : rules.biomeDictionaryBlacklist) {
                BiomeDictionary.Type t = parseBiomeDictionaryType(x);
                if (t != null) rules.resolvedDictBlacklist.add(t);
                else warn(species, "biomeDictionaryBlacklist", x);
            }
        }
    }

    private static BiomeGenBase.TempCategory parseTempCategory(String s) {
        if (s == null) return null;
        String u = s.trim()
            .toUpperCase(Locale.ROOT);
        try {
            return BiomeGenBase.TempCategory.valueOf(u);
        } catch (Exception e) {
            return null;
        }
    }

    private static void warn(BirdSpecies s, String field, String value) {
        //JubitusBirds.LOGGER.warn("[JubitusBirds] Species '{}' has unknown {} entry '{}'.", s.name, field, value);
    }

    private static BiomeManager.BiomeType parseBiomeManagerType(String s) {
        if (s == null) return null;
        String u = s.trim()
            .toUpperCase(Locale.ROOT);
        try {
            return BiomeManager.BiomeType.valueOf(u);
        } catch (Exception e) {
            return null;
        }
    }

    private static BiomeDictionary.Type parseBiomeDictionaryType(String s) {
        if (s == null) return null;

        String u = s.trim()
            .toUpperCase(Locale.ROOT);

        try {
            // Forge 1.12.2 way: creates or retrieves the Type
            return BiomeDictionary.Type.getType(u);
        } catch (Exception e) {
            return null;
        }
    }

}
