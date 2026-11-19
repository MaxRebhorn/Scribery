package de.scribery.core;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/** Schlechtes-Glueck-Schutz fuer Funde (docs/DESIGN-FINDS.md): meist ein Manuskript, das dem Spieler noch fehlt. */
public final class FindLuck {
    /** Anteil der Funde, die bevorzugt ein noch fehlendes Manuskript ziehen. */
    public static final double MISSING_CHANCE = 0.7;

    private FindLuck() {}

    /**
     * Zieht ein Manuskript aus dem Pool: mit der Wahrscheinlichkeit {@code missingChance} eines, das {@code owned} noch nicht
     * hat (gibt es keins mehr, dann irgendeins), sonst irgendeins aus dem ganzen Pool. Leerer Pool: null.
     */
    public static String pick(List<String> pool, Predicate<String> owned, double missingChance, Random random) {
        if (pool.isEmpty()) return null;
        if (random.nextDouble() < missingChance) {
            List<String> missing = pool.stream().filter(owned.negate()).toList();
            if (!missing.isEmpty()) return missing.get(random.nextInt(missing.size()));
        }
        return pool.get(random.nextInt(pool.size()));
    }
}
