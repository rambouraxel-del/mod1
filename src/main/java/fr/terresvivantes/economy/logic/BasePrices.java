package fr.terresvivantes.economy.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Table des prix de base (cuivre par unité, qualité 1), lue depuis {@code base_prices.json}.
 * Le fichier est un objet plat {@code "identifiant": prix}; un identifiant sans espace de noms vaut
 * {@code terresvivantes:<identifiant>}, les autres sont donnés complets (ex. {@code minecraft:wheat}).
 * Logique pure : analyse par expression régulière, sans dépendance JSON.
 */
public final class BasePrices {

    private static final Pattern ENTRY = Pattern.compile("\"([a-z0-9_./:]+)\"\\s*:\\s*(\\d+)");

    private BasePrices() {
    }

    /** Analyse le contenu JSON plat; une clé en double garde la dernière valeur. */
    public static Map<String, Integer> parse(String json) {
        Map<String, Integer> prices = new HashMap<>();
        Matcher m = ENTRY.matcher(json);
        while (m.find()) {
            prices.put(m.group(1), Integer.parseInt(m.group(2)));
        }
        return prices;
    }

    /** Prix de base d'un identifiant, 0 si l'item n'est pas vendable. */
    public static int basePrice(Map<String, Integer> table, String id) {
        return table.getOrDefault(id, 0);
    }
}
