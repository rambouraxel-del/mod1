package fr.terresvivantes.apiculture;

import fr.terresvivantes.apiculture.logic.HoneyType;
import fr.terresvivantes.orchard.OrchardApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Façade publique de l'apiculture. Les autres modules n'appellent que cette classe.
 *
 * <p>Les sources de nectar (fleurs d'un type donné) s'enregistrent ici : le module flore y déclare ses fleurs
 * (trèfle, lavande). Sans enregistrement, une fleur du tag {@code minecraft:flowers} donne du miel sauvage.
 */
public final class ApicultureApi {

    /** Règle : les états qui matchent produisent le type de miel donné. */
    private record NectarRule(Predicate<BlockState> match, HoneyType type) {
    }

    /** Stade de floraison des feuilles fruitières (propriété {@code stage}). */
    private static final int FLOWERING_STAGE = 1;

    private static final List<NectarRule> RULES = new CopyOnWriteArrayList<>();
    private static final Set<ApiaryBlockEntity> APIARIES = ConcurrentHashMap.newKeySet();

    private ApicultureApi() {
    }

    /** Déclare qu'un état (une fleur) apporte un type de nectar. À appeler à l'initialisation du mod. */
    public static void registerNectarSource(Predicate<BlockState> match, HoneyType type) {
        RULES.add(new NectarRule(match, type));
    }

    /**
     * Type de miel apporté par un état : règles enregistrées, puis feuilles fruitières en fleur (orchard),
     * puis fleurs du tag {@code minecraft:flowers} (sauvage). Null si l'état n'apporte rien.
     */
    public static HoneyType nectarOf(BlockState state) {
        for (NectarRule rule : RULES) {
            if (rule.match().test(state)) {
                return rule.type();
            }
        }
        if (OrchardApi.isFruitFoliage(state) && isFloweringFoliage(state)) {
            return HoneyType.ORCHARD;
        }
        if (state.is(BlockTags.FLOWERS)) {
            return HoneyType.WILDFLOWER;
        }
        return null;
    }

    /** Nombre d'apiaires peuplés (population > 0) dans le rayon horizontal de {@code center}. */
    public static int populatedApiaries(ServerLevel level, BlockPos center, int radius) {
        long radiusSquared = (long) radius * radius;
        int count = 0;
        for (ApiaryBlockEntity apiary : APIARIES) {
            if (apiary.getLevel() == level && apiary.isActive()
                    && apiary.getBlockPos().distSqr(center) <= radiusSquared) {
                count++;
            }
        }
        return count;
    }

    static void track(ApiaryBlockEntity apiary) {
        APIARIES.add(apiary);
    }

    static void untrack(ApiaryBlockEntity apiary) {
        APIARIES.remove(apiary);
    }

    private static boolean isFloweringFoliage(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty("stage");
        return property instanceof IntegerProperty stage && state.getValue(stage) == FLOWERING_STAGE;
    }
}
