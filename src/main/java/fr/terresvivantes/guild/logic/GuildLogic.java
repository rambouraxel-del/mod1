package fr.terresvivantes.guild.logic;

import fr.terresvivantes.economy.logic.ReputationLogic;

/**
 * Règles de la guilde (v0.7) : niveaux d'échange et paliers de réputation. Logique pure, sans import net.minecraft.
 *
 * <p>Le niveau {@code n} (1 à 5) d'une offre exige le palier d'index {@code n - 1} de {@link ReputationLogic.Tier} :
 * niveau 1 Inconnu, niveau 2 Habitué, niveau 3 Fournisseur, niveau 4 Producteur réputé, niveau 5 Maître fermier.
 */
public final class GuildLogic {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;

    private GuildLogic() {
    }

    /** Index du palier (dans {@link ReputationLogic.Tier}) exigé par un niveau d'échange, borné à 1..5. */
    public static int requiredTierIndex(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level)) - 1;
    }

    /** Vrai si l'offre de ce niveau est bloquée pour un joueur de ce palier. */
    public static boolean isLocked(int level, ReputationLogic.Tier tier) {
        return requiredTierIndex(level) > tier.ordinal();
    }
}
