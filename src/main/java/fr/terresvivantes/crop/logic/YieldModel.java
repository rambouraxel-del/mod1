package fr.terresvivantes.crop.logic;

/** Rendement d'une récolte : nombre de produits après modifiers. Aucun import net.minecraft. */
public final class YieldModel {

    private YieldModel() {
    }

    /**
     * Nombre de produits après multiplication par {@code factor}, arrondi probabiliste : la partie
     * entière est garantie, la partie fractionnaire donne un produit de plus avec cette probabilité.
     *
     * @param count  nombre de produits avant modifiers (positif ou nul)
     * @param factor facteur de rendement (négatif ramené à 0)
     * @param roll   tirage uniforme dans [0, 1[
     */
    public static int scaledCount(int count, float factor, double roll) {
        if (count <= 0) {
            return 0;
        }
        double value = count * Math.max(0.0, factor);
        int base = (int) Math.floor(value);
        double fraction = value - base;
        return base + (roll < fraction ? 1 : 0);
    }
}
