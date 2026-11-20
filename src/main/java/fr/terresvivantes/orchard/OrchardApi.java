package fr.terresvivantes.orchard;

import fr.terresvivantes.crop.logic.YieldModel;
import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

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

    /**
     * Multiplicateur du nombre de fruits récoltés sur une feuille mûre (contrat v0.3). 1.0 neutre.
     */
    @FunctionalInterface
    public interface FruitYieldModifier {
        float modify(ServerLevel level, BlockPos pos, FruitTreeDefinition def);
    }

    private static final List<FruitSetModifier> FRUIT_SET_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<FruitYieldModifier> FRUIT_YIELD_MODIFIERS = new CopyOnWriteArrayList<>();

    private OrchardApi() {
    }

    /**
     * Enregistre un multiplicateur de fructification. Destiné à la pollinisation : un module (abeilles, ruches)
     * peut augmenter la chance de nouer les fruits à proximité. À appeler pendant l'initialisation du mod.
     */
    public static void registerFruitSetModifier(FruitSetModifier modifier) {
        FRUIT_SET_MODIFIERS.add(modifier);
    }

    /** Enregistre un multiplicateur de rendement des fruits (v0.3, pollinisation). À appeler à l'initialisation. */
    public static void registerFruitYieldModifier(FruitYieldModifier modifier) {
        FRUIT_YIELD_MODIFIERS.add(modifier);
    }

    /** Vrai si l'état est un feuillage fruitier (tous les stades, toutes espèces). */
    public static boolean isFruitFoliage(BlockState state) {
        return state.getBlock() instanceof FruitLeavesBlock;
    }

    /**
     * Récolte la feuille fruitière mûre (stade 3) à cette position, comme le clic droit à main vide : la feuille
     * revient au stade 0 et les fruits sont rendus. Liste vide si ce n'est pas un feuillage fruitier mûr.
     * Les fruits ne sont pas rangés ici : l'appelant (joueur, récolteuse) les insère dans son inventaire.
     */
    public static List<ItemStack> harvestRipeLeaves(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FruitLeavesBlock leaves)) {
            return List.of();
        }
        return leaves.harvestRipe(level, pos, state);
    }

    /** Produit des multiplicateurs de fructification enregistrés à cette position pour une espèce. */
    static float fruitSetFactor(ServerLevel level, BlockPos pos, FruitTreeDefinition def) {
        float factor = 1.0f;
        for (FruitSetModifier modifier : FRUIT_SET_MODIFIERS) {
            factor *= Math.max(0f, modifier.modify(level, pos, def));
        }
        return factor;
    }

    /** Produit des multiplicateurs de rendement enregistrés à cette position pour une espèce. */
    static float fruitYieldFactor(ServerLevel level, BlockPos pos, FruitTreeDefinition def) {
        float factor = 1.0f;
        for (FruitYieldModifier modifier : FRUIT_YIELD_MODIFIERS) {
            factor *= Math.max(0f, modifier.modify(level, pos, def));
        }
        return factor;
    }

    /** Applique le rendement enregistré au nombre de fruits d'une récolte (arrondi probabiliste). */
    static int scaleFruitYield(ServerLevel level, BlockPos pos, FruitTreeDefinition def, int count) {
        return YieldModel.scaledCount(count, fruitYieldFactor(level, pos, def), level.getRandom().nextDouble());
    }
}
