package fr.terresvivantes.livestock.logic;

import java.util.Locale;

/** État sanitaire d'un animal. Aucun import net.minecraft. Les maladies sont posées par le module vet. */
public enum Ailment {
    NONE, SICK, PARASITES;

    /** Nom sérialisé vers l'enum; inconnu ou vide donne {@link #NONE}. */
    public static Ailment fromName(String name) {
        if (name == null) {
            return NONE;
        }
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
