package fr.terresvivantes.orchard;

import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Façade publique du module orchard. Les autres modules n'appellent que cette classe.
 */
public final class OrchardApi {

    /**
     * Multiplicateur de la chance de nouer les fruits (transition des feuilles du stade 1 au stade 2).
     * Renvoie 1.0 pour un effet neutre, plus de 1.0 pour favoriser la fructification, 0.0 pour l'empêcher.
     */
    @FunctionalInterface
    public interface FruitSetModifier {
        float modify(ServerLevel level, BlockPos pos, FruitTreeDefinition def);
    }

    private static final List<FruitSetModifier> FRUIT_SET_MODIFIERS = new CopyOnWriteArrayList<>();

    private OrchardApi() {
    }

    /**
     * Enregistre un multiplicateur de fructification. Destiné à la pollinisation : un module (abeilles, ruches)
     * peut augmenter la chance de nouer les fruits à proximité. À appeler pendant l'initialisation du mod.
     */
    public static void registerFruitSetModifier(FruitSetModifier modifier) {
        FRUIT_SET_MODIFIERS.add(modifier);
    }

    /** Produit des multiplicateurs enregistrés à cette position pour une espèce. */
    static float fruitSetFactor(ServerLevel level, BlockPos pos, FruitTreeDefinition def) {
        float factor = 1.0f;
        for (FruitSetModifier modifier : FRUIT_SET_MODIFIERS) {
            factor *= Math.max(0f, modifier.modify(level, pos, def));
        }
        return factor;
    }
}
