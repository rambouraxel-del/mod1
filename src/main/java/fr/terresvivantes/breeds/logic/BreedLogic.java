package fr.terresvivantes.breeds.logic;

import fr.terresvivantes.livestock.logic.LivestockLogic;

import java.util.List;

/**
 * Règles des races : attribution selon le climat, hérédité, effets génétiques (production, viande, œufs, hiver).
 * Les tirages reçoivent un nombre dans [0, 1[ (tests déterministes possibles). Aucun import net.minecraft.
 */
public final class BreedLogic {

    /** Température de base de biome en dessous de laquelle le climat est froid. */
    public static final float COLD_BELOW = 0.2F;
    /** Température de base de biome à partir de laquelle le climat est chaud. */
    public static final float WARM_FROM = 1.0F;
    /** Chance d'un œuf supplémentaire pour une poule Marans. */
    public static final double MARANS_EXTRA_EGG_CHANCE = 0.10;
    /** Multiplicateur de production de la Holstein (lait). */
    public static final float HOLSTEIN_PRODUCTION = 1.2F;
    /** Multiplicateur de production de la Charolais. */
    public static final float CHAROLAIS_PRODUCTION = 0.9F;
    /** Multiplicateur de ponte de la Leghorn. */
    public static final float LEGHORN_PRODUCTION = 1.25F;
    /** Morceaux de viande supplémentaires de la Charolais à l'abattage. */
    public static final int CHAROLAIS_EXTRA_MEAT = 1;

    /** Climat d'un lieu, d'après la température de base du biome. */
    public enum Climate { COLD, TEMPERATE, WARM }

    private BreedLogic() {
    }

    public static Climate climate(float biomeBaseTemperature) {
        if (biomeBaseTemperature < COLD_BELOW) {
            return Climate.COLD;
        }
        return biomeBaseTemperature >= WARM_FROM ? Climate.WARM : Climate.TEMPERATE;
    }

    /**
     * Poids d'attribution d'une race selon le climat. Vaches : la Highland est propre au froid. Poules : la Sussex
     * craint la chaleur, la Leghorn est la race des régions chaudes.
     */
    public static int weight(Breed breed, Climate climate) {
        return switch (breed) {
            case HOLSTEIN -> climate == Climate.COLD ? 2 : climate == Climate.WARM ? 3 : 4;
            case CHAROLAIS -> climate == Climate.COLD ? 2 : 4;
            case HIGHLAND -> climate == Climate.COLD ? 6 : 1;
            case MARANS -> climate == Climate.COLD || climate == Climate.WARM ? 3 : 4;
            case LEGHORN -> climate == Climate.COLD ? 1 : climate == Climate.WARM ? 6 : 2;
            case SUSSEX -> climate == Climate.COLD ? 6 : climate == Climate.WARM ? 1 : 2;
        };
    }

    /** Race tirée au hasard pondéré par le climat. {@code roll} dans [0, 1[. */
    public static Breed pick(Species species, Climate climate, double roll) {
        List<Breed> breeds = Breed.ofSpecies(species);
        int total = 0;
        for (Breed breed : breeds) {
            total += weight(breed, climate);
        }
        int target = (int) Math.floor(clamp01(roll) * total);
        for (Breed breed : breeds) {
            target -= weight(breed, climate);
            if (target < 0) {
                return breed;
            }
        }
        return breeds.get(breeds.size() - 1);
    }

    /**
     * Race d'un enfant : celle d'un des deux parents, à 50/50. Un parent sans race cède la place à l'autre.
     * Renvoie {@code null} si aucun parent n'a de race (l'attribution par climat prend le relais).
     */
    public static Breed inherit(Breed parentA, Breed parentB, double roll) {
        if (parentA == null) {
            return parentB;
        }
        if (parentB == null) {
            return parentA;
        }
        return clamp01(roll) < 0.5 ? parentA : parentB;
    }

    /** Multiplicateur de production propre à la race (1.0 si aucun effet). */
    public static float productionMultiplier(Breed breed) {
        if (breed == null) {
            return 1.0F;
        }
        return switch (breed) {
            case HOLSTEIN -> HOLSTEIN_PRODUCTION;
            case CHAROLAIS -> CHAROLAIS_PRODUCTION;
            case LEGHORN -> LEGHORN_PRODUCTION;
            default -> 1.0F;
        };
    }

    /** Morceaux de viande supplémentaires à l'abattage. */
    public static int extraMeat(Breed breed) {
        return breed == Breed.CHAROLAIS ? CHAROLAIS_EXTRA_MEAT : 0;
    }

    /** Vrai si la poule pond un œuf supplémentaire ce tour-ci (Marans, 10 %). */
    public static boolean extraEgg(Breed breed, double roll) {
        return breed == Breed.MARANS && clamp01(roll) < MARANS_EXTRA_EGG_CHANCE;
    }

    /**
     * Satiété compensée à chaque cycle d'hiver pour la Highland : la différence entre la consommation hivernale et la
     * consommation normale, ce qui annule le malus d'hiver sur la faim.
     */
    public static int winterFeedCompensation() {
        return Math.round(LivestockLogic.CONSUMPTION * (float) LivestockLogic.WINTER_CONSUMPTION_FACTOR)
                - LivestockLogic.CONSUMPTION;
    }

    /** Vrai si la race annule le malus d'hiver sur la faim (Highland). */
    public static boolean cancelsWinterHunger(Breed breed) {
        return breed == Breed.HIGHLAND;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(Math.nextDown(1.0), value));
    }
}
