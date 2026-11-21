package fr.terresvivantes.crop;

import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.GrowthDecision;
import fr.terresvivantes.crop.logic.GrowthModel;
import fr.terresvivantes.crop.logic.YieldModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Points d'extension du module crop (contrat v0.2). Les autres modules s'y branchent au chargement du mod.
 *
 * <ul>
 *   <li>{@link GrowthModifier} : multiplie le facteur total de croissance (cultures du mod et vanilla).</li>
 *   <li>{@link GrowthListener} : reçoit chaque décision de croissance, après calcul.</li>
 *   <li>{@link YieldModifier} : multiplie le nombre de produits récoltés (arrondi probabiliste).</li>
 *   <li>{@link PlantListener} : reçoit chaque plantation ou replantation.</li>
 * </ul>
 *
 * Les listes sont des {@link CopyOnWriteArrayList} : l'enregistrement se fait au démarrage, la lecture en jeu.
 * {@code def} est {@code null} pour une culture vanilla.
 */
public final class CropGrowthHooks {

    /** Modifie le facteur de croissance. Les modifiers se multiplient entre eux (valeurs négatives ramenées à 0). */
    @FunctionalInterface
    public interface GrowthModifier {
        float modify(ServerLevel level, BlockPos cropPos, BlockState state, CropDefinition def);
    }

    /** Observe une décision de croissance, après application de tous les modifiers. */
    @FunctionalInterface
    public interface GrowthListener {
        void onGrowthAttempt(ServerLevel level, BlockPos cropPos, BlockState state, CropDefinition def,
                             GrowthDecision decision, float factor);
    }

    /** Modifie le nombre de produits d'une récolte. Les modifiers se multiplient entre eux. */
    @FunctionalInterface
    public interface YieldModifier {
        float modify(ServerLevel level, BlockPos cropPos, BlockState state, CropDefinition def);
    }

    /** Observe une plantation ou une replantation (culture du mod ou vanilla). */
    @FunctionalInterface
    public interface PlantListener {
        void onPlanted(ServerLevel level, BlockPos pos, BlockState state);
    }

    private static final List<GrowthModifier> GROWTH_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<GrowthListener> GROWTH_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<YieldModifier> YIELD_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<PlantListener> PLANT_LISTENERS = new CopyOnWriteArrayList<>();

    private CropGrowthHooks() {
    }

    public static void registerModifier(GrowthModifier modifier) {
        GROWTH_MODIFIERS.add(Objects.requireNonNull(modifier));
    }

    public static void registerListener(GrowthListener listener) {
        GROWTH_LISTENERS.add(Objects.requireNonNull(listener));
    }

    public static void registerYieldModifier(YieldModifier modifier) {
        YIELD_MODIFIERS.add(Objects.requireNonNull(modifier));
    }

    public static void registerPlantListener(PlantListener listener) {
        PLANT_LISTENERS.add(Objects.requireNonNull(listener));
    }

    /**
     * Facteur multiplicatif de tous les {@link GrowthModifier} enregistrés pour une culture (1.0 sans modifier).
     * Public pour les cultures hors de ce module (ex. le riz, rizière), qui gardent leur propre formule de base et
     * appliquent ensuite ce produit. {@code def} est {@code null} pour une culture vanilla ou sans définition.
     */
    public static float applyModifiers(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def) {
        return growthModifierProduct(level, pos, state, def);
    }

    /** Produit de tous les modifiers de croissance (1.0 s'il n'y en a aucun). */
    static float growthModifierProduct(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def) {
        float[] values = new float[GROWTH_MODIFIERS.size()];
        int i = 0;
        for (GrowthModifier modifier : GROWTH_MODIFIERS) {
            values[i++] = modifier.modify(level, pos, state, def);
        }
        return GrowthModel.combineModifiers(values);
    }

    /** Produit de tous les modifiers de rendement (1.0 s'il n'y en a aucun). Public : voir {@link #scaleYield}. */
    public static float yieldModifierProduct(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def) {
        float[] values = new float[YIELD_MODIFIERS.size()];
        int i = 0;
        for (YieldModifier modifier : YIELD_MODIFIERS) {
            values[i++] = modifier.modify(level, pos, state, def);
        }
        return GrowthModel.combineModifiers(values);
    }

    /**
     * Applique le rendement à un nombre de produits, avec arrondi probabiliste. Public pour les récoltes hors
     * {@code TVCropBlock} (riz, vanilla via {@link CropApi#harvestAndReplant}).
     */
    public static int scaleYield(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def, int count) {
        float factor = yieldModifierProduct(level, pos, state, def);
        return YieldModel.scaledCount(count, factor, level.getRandom().nextDouble());
    }

    /** Notifie les {@link GrowthListener} d'une tentative de croissance. Appelé par chaque type de culture. */
    public static void notifyGrowthAttempt(ServerLevel level, BlockPos pos, BlockState state, CropDefinition def,
                                    GrowthDecision decision, float factor) {
        for (GrowthListener listener : GROWTH_LISTENERS) {
            listener.onGrowthAttempt(level, pos, state, def, decision, factor);
        }
    }

    /** Notifie les {@link PlantListener} d'une plantation. Appelé à chaque plantation (culture du mod, vanilla, riz). */
    public static void notifyPlanted(ServerLevel level, BlockPos pos, BlockState state) {
        for (PlantListener listener : PLANT_LISTENERS) {
            listener.onPlanted(level, pos, state);
        }
    }
}
