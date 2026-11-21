package fr.terresvivantes.breeds.logic;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Races d'élevage (vaches et poules). Identifiant stable en minuscules, utilisé dans la sauvegarde, le réseau et les
 * clés de traduction. Aucun import net.minecraft.
 */
public enum Breed {
    HOLSTEIN(Species.COW, "holstein"),
    HIGHLAND(Species.COW, "highland"),
    CHAROLAIS(Species.COW, "charolais"),
    MARANS(Species.CHICKEN, "marans"),
    LEGHORN(Species.CHICKEN, "leghorn"),
    SUSSEX(Species.CHICKEN, "sussex");

    private final Species species;
    private final String id;

    Breed(Species species, String id) {
        this.species = species;
        this.id = id;
    }

    public Species species() {
        return species;
    }

    public String id() {
        return id;
    }

    /** Race d'après son identifiant (insensible à la casse). */
    public static Optional<Breed> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(breed -> breed.id.equals(wanted)).findFirst();
    }

    /** Races d'une espèce, dans l'ordre de déclaration. */
    public static List<Breed> ofSpecies(Species species) {
        return Arrays.stream(values()).filter(breed -> breed.species == species).toList();
    }
}
