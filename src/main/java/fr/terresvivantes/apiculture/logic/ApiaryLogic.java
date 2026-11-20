package fr.terresvivantes.apiculture.logic;

import fr.terresvivantes.season.logic.Season;

/** Règles de l'apiaire (population, production, peuplement, colère). Logique pure, sans Minecraft. */
public final class ApiaryLogic {

    public static final int MAX_POPULATION = 100;
    public static final int FRAME_SLOTS = 4;
    /** Une période de l'apiaire : 200 ticks. */
    public static final int CYCLE_TICKS = 200;
    public static final int FLOWER_RADIUS = 6;
    /** Nombre de fleurs (et de feuilles en fleur) pour des ressources florales complètes. */
    public static final int FLOWER_SATURATION = 8;
    public static final int ABSORB_RADIUS = 2;
    public static final int ABSORBED_POPULATION = 10;
    public static final int SETTLED_POPULATION = 5;
    public static final int SETTLE_RADIUS = 8;
    /** Un jour de Minecraft : délai avant le peuplement depuis une ruche vanilla voisine. */
    public static final long SETTLE_DELAY_TICKS = 24000L;
    /** Chance par période d'une naissance avec ressources florales complètes au printemps. */
    public static final double GROWTH_CHANCE = 0.1;
    /** Chance par période de perdre une abeille sans fleurs. */
    public static final double DECLINE_CHANCE = 0.05;
    /** Progression d'un cadre par période, à population pleine et ressources complètes (une demi-journée). */
    public static final double PRODUCTION_PER_CYCLE = 1.0 / 60.0;
    public static final int POLLINATOR_RADIUS = 24;
    public static final float POLLINATOR_PER_APIARY = 0.25f;
    public static final float POLLINATOR_CAP = 0.5f;
    public static final float HABITAT_BONUS = 0.05f;
    public static final int ANGRY_BEES = 2;
    /** Durée de colère d'une abeille vanilla lancée contre le joueur : 20 secondes. */
    public static final int ANGER_TICKS = 400;
    public static final int SMOKER_RADIUS = 8;

    private ApiaryLogic() {
    }

    /** Multiplicateur saisonnier de la production et de la croissance; 0 = dormance. */
    public static float seasonFactor(Season season) {
        return switch (season) {
            case SPRING, SUMMER -> 1f;
            case AUTUMN -> 0.5f;
            case WINTER -> 0f;
        };
    }

    /** Ressources florales dans [0, 1], proportionnelles au nombre de fleurs. */
    public static float flowerFactor(int flowers) {
        return Math.min(1f, flowers / (float) FLOWER_SATURATION);
    }

    /** Nombre total de fleurs : somme des comptes par type. */
    public static int flowerTotal(int[] countsByType) {
        int total = 0;
        for (int count : countsByType) {
            total += count;
        }
        return total;
    }

    /** Type dominant (le plus de fleurs); égalité : premier dans l'ordre de l'enum. Null sans fleur. */
    public static HoneyType dominantHoney(int[] countsByType) {
        HoneyType best = null;
        int bestCount = 0;
        for (HoneyType type : HoneyType.values()) {
            int count = countsByType[type.ordinal()];
            if (count > bestCount) {
                bestCount = count;
                best = type;
            }
        }
        return best;
    }

    /**
     * Population après une période. {@code roll} est un tirage dans [0, 1[. Hiver : population stable.
     * Avec fleurs : naissance possible. Sans fleur : déclin possible.
     */
    public static int nextPopulation(int population, int flowers, float season, double roll) {
        if (season <= 0f) {
            return population;
        }
        if (flowers > 0) {
            double chance = GROWTH_CHANCE * flowerFactor(flowers) * season;
            return roll < chance ? Math.min(MAX_POPULATION, population + 1) : population;
        }
        return roll < DECLINE_CHANCE ? Math.max(0, population - 1) : population;
    }

    /** Progression d'un cadre pendant une période : proportionnelle à la population et aux fleurs. */
    public static float productionIncrement(int population, int flowers, float season) {
        return (population / (float) MAX_POPULATION) * flowerFactor(flowers) * season
                * (float) PRODUCTION_PER_CYCLE;
    }

    public static float advance(float progress, float increment) {
        return Math.min(1f, progress + increment);
    }

    public static boolean isComplete(float progress) {
        return progress >= 1f;
    }

    /** Un apiaire vide absorbe une abeille proche avec une chance donnée (hors dormance). */
    public static boolean shouldAbsorb(int population, float season, int beesNearby, double roll, double chance) {
        return population == 0 && season > 0f && beesNearby > 0 && roll < chance;
    }

    /** Récolte sans calme : les abeilles se fâchent si la colonie dépasse le seuil. */
    public static boolean angersBees(int population, int threshold, boolean calm) {
        return !calm && population > threshold;
    }

    /** Apport de pollinisation des apiaires peuplés dans le rayon, plafonné. */
    public static float pollinatorContribution(int populatedApiaries) {
        return Math.min(POLLINATOR_CAP, populatedApiaries * POLLINATOR_PER_APIARY);
    }

    /** Habitat ajouté à l'indice de biodiversité : un bonus fixe dès qu'un apiaire peuplé est proche. */
    public static float habitatContribution(int populatedApiaries) {
        return populatedApiaries > 0 ? HABITAT_BONUS : 0f;
    }
}
