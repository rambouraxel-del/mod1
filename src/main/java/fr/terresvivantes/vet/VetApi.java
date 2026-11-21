package fr.terresvivantes.vet;

import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.vet.logic.VetLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.DoubleSupplier;

/**
 * Façade du module vet. Les autres modules passent par cette classe.
 */
public final class VetApi {

    /**
     * Multiplicateur de la chance de maladie par cycle d'un animal (v0.9). 1.0 neutre; ex. la race Sussex (poules)
     * ×0.7. Appelé à chaque évaluation de maladie, côté serveur.
     */
    @FunctionalInterface
    public interface DiseaseChanceModifier {
        float modify(Animal animal);
    }

    private static final List<DiseaseChanceModifier> DISEASE_MODIFIERS = new CopyOnWriteArrayList<>();

    private static volatile DoubleSupplier random = Math::random;

    private VetApi() {
    }

    /** Enregistre un modificateur de chance de maladie (à l'initialisation du mod). */
    public static void registerDiseaseChanceModifier(DiseaseChanceModifier modifier) {
        DISEASE_MODIFIERS.add(Objects.requireNonNull(modifier));
    }

    /** Produit des modificateurs de maladie enregistrés pour cet animal (1.0 sans modificateur). */
    public static float diseaseChanceFactor(Animal animal) {
        float factor = 1.0f;
        for (DiseaseChanceModifier modifier : DISEASE_MODIFIERS) {
            factor *= Math.max(0f, modifier.modify(animal));
        }
        return factor;
    }

    /** Tirage aléatoire dans [0, 1[ utilisé par les maladies. */
    static double roll() {
        return random.getAsDouble();
    }

    /**
     * Remplace la source de tirages (tests de jeu, déterministes). A remettre avec {@link #resetRandom()}.
     */
    public static void setRandomForTest(DoubleSupplier source) {
        random = source;
    }

    /** Rétablit la source de tirages aléatoire par défaut. */
    public static void resetRandom() {
        random = Math::random;
    }

    /** Vrai s'il y a un bloc de sel à moins de {@link VetLogic#SALT_LICK_RADIUS} blocs (hauteur ±2). */
    public static boolean isNearSaltLick(ServerLevel level, BlockPos center) {
        BlockPos min = center.offset(-VetLogic.SALT_LICK_RADIUS, -2, -VetLogic.SALT_LICK_RADIUS);
        BlockPos max = center.offset(VetLogic.SALT_LICK_RADIUS, 2, VetLogic.SALT_LICK_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(VetModule.SALT_LICK.get())) {
                return true;
            }
        }
        return false;
    }

    /** Soigne l'animal : état sanitaire NONE, santé +20 (trousse vétérinaire). */
    public static void treat(Animal animal) {
        LivestockApi.setState(animal, VetLogic.treat(LivestockApi.getState(animal)));
    }
}
