package fr.terresvivantes.compost.logic;

/**
 * Logique pure du compost : remplissage du bac, maturation, recolte, fumier et fertilisants.
 * Aucun import net.minecraft: testable en JUnit.
 */
public final class CompostLogic {

    /** Niveau maximal du bac a compost. */
    public static final int MAX_LEVEL = 8;
    /** Niveaux apportes par une matiere organique (produit, graine, feuille, herbe). */
    public static final int ORGANIC_LEVELS = 1;
    /** Niveaux apportes par une unite de fumier. */
    public static final int MANURE_LEVELS = 2;
    /** Phosphore ajoute par bone meal sur une terre labouree. */
    public static final int BONE_MEAL_PHOSPHORUS = 15;
    /** Intervalle entre deux verifications de fumier par animal. */
    public static final int MANURE_CHECK_INTERVAL_TICKS = 1200;

    /** Duree d'un jour Minecraft en ticks. */
    private static final double DAY_TICKS = 24000.0;
    /** Ticks aleatoires par section de 16x16x16 et par tick (gamerule randomTickSpeed par defaut). */
    private static final double RANDOM_TICK_SPEED = 3.0;
    /** Blocs par section (16 x 16 x 16). */
    private static final double BLOCKS_PER_SECTION = 4096.0;

    /** Probabilite de maturation a chaque randomTick, pour une maturation moyenne d'un jour MC. */
    public static final double MATURATION_CHANCE = 1.0 / (DAY_TICKS * RANDOM_TICK_SPEED / BLOCKS_PER_SECTION);

    private CompostLogic() {
    }

    /** Fertilisants et leurs effets sur le sol (valeurs ajoutees, bornees ensuite a 100). */
    public enum Fertilizer {
        COMPOST(15, 15, 15, 20),
        MANURE(25, 0, 0, 10),
        WOOD_ASH(0, 0, 20, 0);

        private final int nitrogen;
        private final int phosphorus;
        private final int potassium;
        private final int organic;

        Fertilizer(int nitrogen, int phosphorus, int potassium, int organic) {
            this.nitrogen = nitrogen;
            this.phosphorus = phosphorus;
            this.potassium = potassium;
            this.organic = organic;
        }

        public int nitrogen() {
            return nitrogen;
        }

        public int phosphorus() {
            return phosphorus;
        }

        public int potassium() {
            return potassium;
        }

        public int organic() {
            return organic;
        }
    }

    /** Niveaux apportes par une matiere : fumier 2, matiere organique 1, autre 0. */
    public static int levelsFor(boolean manure, boolean organic) {
        if (manure) {
            return MANURE_LEVELS;
        }
        if (organic) {
            return ORGANIC_LEVELS;
        }
        return 0;
    }

    /** Ajoute des niveaux sans depasser le maximum. */
    public static int addLevels(int current, int added) {
        return Math.min(MAX_LEVEL, current + Math.max(0, added));
    }

    /** Vrai si le bac est plein et pas encore pret : un randomTick peut le faire maturer. */
    public static boolean shouldMature(int level, boolean ready, double roll) {
        return level >= MAX_LEVEL && !ready && roll < MATURATION_CHANCE;
    }

    /** Quantite de compost recoltee : 2 ou 3. */
    public static int harvestAmount(double roll) {
        return roll < 0.5 ? 2 : 3;
    }

    /** Vrai si un animal lache du fumier a cette verification. */
    public static boolean dropsManure(double roll, double chance) {
        return roll < chance;
    }

    /** Vrai si l'entite doit etre verifiee a ce tick (decale par son id pour etaler la charge). */
    public static boolean isManureCheckTick(int tickCount, int entityId) {
        return Math.floorMod(tickCount + entityId, MANURE_CHECK_INTERVAL_TICKS) == 0;
    }
}
