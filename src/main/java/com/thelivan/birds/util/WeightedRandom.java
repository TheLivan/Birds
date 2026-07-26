package com.thelivan.birds.util;

import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/** Picks a random element from a list, weighted by a per-element positive weight. */
public final class WeightedRandom {

    private WeightedRandom() {}

    /**
     * Returns a weighted-random pick from {@code items}, or {@code null} if the list is empty or every weight is
     * {@code <= 0}.
     */
    public static <T> T pick(List<T> items, ToDoubleFunction<T> weightFn, Random random) {
        double totalWeight = 0.0;
        for (T item : items) totalWeight += weightFn.applyAsDouble(item);
        if (totalWeight <= 0) return null;

        double pick = random.nextDouble() * totalWeight;
        double acc = 0.0;
        for (T item : items) {
            acc += weightFn.applyAsDouble(item);
            if (pick < acc) return item;
        }

        return items.get(items.size() - 1);
    }
}
