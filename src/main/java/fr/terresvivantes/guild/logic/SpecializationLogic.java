package fr.terresvivantes.guild.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Titres de spécialisation (v0.7) : compteurs de ventes au marchand de la guilde par filière. À 256 unités le titre
 * est obtenu; il donne +5 % de chance de qualité supérieure aux machines de la filière. Logique pure, sans import
 * net.minecraft. Les identifiants d'items sont complets ({@code espace:chemin}).
 */
public final class SpecializationLogic {

    /** Unités à vendre dans une filière pour obtenir son titre. */
    public static final int THRESHOLD = 256;
    /** Chance supplémentaire qu'une sortie de machine de la filière soit de qualité +1. */
    public static final float QUALITY_BONUS_CHANCE = 0.05f;

    private static final String MOD = "terresvivantes:";
    private static final Map<String, Filiere> BY_ITEM = buildIndex();

    /** Filières et leur identifiant stable (clé des compteurs et des traductions). */
    public enum Filiere {
        MARAICHER("maraicher"),
        ARBORICULTEUR("arboriculteur"),
        ELEVEUR("eleveur"),
        FROMAGER("fromager"),
        APICULTEUR("apiculteur");

        private final String id;

        Filiere(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    private SpecializationLogic() {
    }

    /** Filière d'un item vendu, s'il en a une. */
    public static Optional<Filiere> filiereOf(String itemId) {
        return Optional.ofNullable(BY_ITEM.get(itemId));
    }

    /** Filière d'après son identifiant ({@code "maraicher"}, etc.). */
    public static Optional<Filiere> byId(String id) {
        for (Filiere filiere : Filiere.values()) {
            if (filiere.id().equals(id)) {
                return Optional.of(filiere);
            }
        }
        return Optional.empty();
    }

    /** Compteur après ajout d'unités, borné à {@link Integer#MAX_VALUE} et jamais négatif. */
    public static int add(int current, int units) {
        long sum = (long) Math.max(0, current) + Math.max(0, units);
        return (int) Math.min(Integer.MAX_VALUE, sum);
    }

    /** Vrai si le compteur vient de franchir le seuil (titre obtenu par cette vente). */
    public static boolean reachedThreshold(int before, int after) {
        return before < THRESHOLD && after >= THRESHOLD;
    }

    /** Vrai si le compteur donne droit au titre. */
    public static boolean hasTitle(int count) {
        return count >= THRESHOLD;
    }

    private static Map<String, Filiere> buildIndex() {
        Map<String, Filiere> index = new HashMap<>();
        // Maraîcher : cultures.
        for (String crop : new String[] {"tomato", "corn", "soybean", "lettuce", "onion", "garlic", "bell_pepper",
                "eggplant", "strawberry", "barley", "cabbage", "cucumber"}) {
            index.put(MOD + crop, Filiere.MARAICHER);
        }
        for (String vanilla : new String[] {"minecraft:wheat", "minecraft:carrot", "minecraft:potato",
                "minecraft:beetroot"}) {
            index.put(vanilla, Filiere.MARAICHER);
        }
        // Arboriculteur : fruits des vergers.
        index.put("minecraft:apple", Filiere.ARBORICULTEUR);
        for (String fruit : new String[] {"cherry", "orange", "olive", "walnut", "peach"}) {
            index.put(MOD + fruit, Filiere.ARBORICULTEUR);
        }
        // Éleveur : produits animaux (hors fromages).
        for (String animal : new String[] {"duck_egg", "raw_duck", "cream", "butter"}) {
            index.put(MOD + animal, Filiere.ELEVEUR);
        }
        index.put("minecraft:egg", Filiere.ELEVEUR);
        index.put("minecraft:milk_bucket", Filiere.ELEVEUR);
        // Fromager : fromages.
        for (String cheese : new String[] {"cheese_curd", "young_cheese", "goat_cheese", "aged_cheese", "cheese_slice"}) {
            index.put(MOD + cheese, Filiere.FROMAGER);
        }
        // Apiculteur : miels.
        for (String honey : new String[] {"wildflower_honey", "clover_honey", "lavender_honey", "orchard_honey"}) {
            index.put(MOD + honey, Filiere.APICULTEUR);
        }
        return Map.copyOf(index);
    }
}
