package fr.terresvivantes.orchard;

import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.Supplier;

/**
 * Arbre fruitier sauvage. Réutilise le générateur du sapling. Placé par la configuration de biome
 * (data/terresvivantes/worldgen et neoforge/biome_modifier) avec une rareté d'environ un arbre pour 8 chunks.
 */
public final class WildFruitTreeFeature extends Feature<NoneFeatureConfiguration> {

    private final FruitTreeDefinition def;
    private final Supplier<FruitLeavesBlock> leaves;

    public WildFruitTreeFeature(FruitTreeDefinition def, Supplier<FruitLeavesBlock> leaves) {
        super(NoneFeatureConfiguration.CODEC);
        this.def = def;
        this.leaves = leaves;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!OrchardTrees.isGround(level.getBlockState(origin.below()))) {
            return false;
        }
        return OrchardTrees.grow(level, origin, def, leaves.get(), context.random());
    }
}
