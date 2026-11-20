package fr.terresvivantes.orchard;

import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.OrchardLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Génération d'un arbre fruitier, partagée par le sapling (croissance, bonemeal) et la génération naturelle. */
final class OrchardTrees {

    private OrchardTrees() {
    }

    /** Terre où un arbre fruitier peut pousser. */
    static boolean isGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.FARMLAND);
    }

    /** Tronc vanilla de l'espèce, résolu depuis son identifiant. */
    static Block trunkBlock(FruitTreeDefinition def) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(def.trunkLog()));
    }

    /**
     * Fait pousser un arbre à partir de {@code origin} (le bloc du sapling ou la première case au-dessus du sol).
     * Ne pousse que si l'espace du tronc et de la couronne est libre (air ou remplaçable). Retourne vrai si l'arbre est posé.
     */
    static boolean grow(LevelAccessor level, BlockPos origin, FruitTreeDefinition def,
                        FruitLeavesBlock leaves, RandomSource random) {
        OrchardLogic.TreeShape shape = OrchardLogic.treeShape(OrchardLogic.trunkHeight(random.nextDouble()));
        for (OrchardLogic.Offset offset : shape.trunk()) {
            if (offset.y() > 0 && !isFree(level, origin.offset(offset.x(), offset.y(), offset.z()))) {
                return false;
            }
        }
        for (OrchardLogic.Offset offset : shape.leaves().keySet()) {
            if (!isFree(level, origin.offset(offset.x(), offset.y(), offset.z()))) {
                return false;
            }
        }
        Block trunk = trunkBlock(def);
        for (OrchardLogic.Offset offset : shape.trunk()) {
            level.setBlock(origin.offset(offset.x(), offset.y(), offset.z()), trunk.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        for (var entry : shape.leaves().entrySet()) {
            OrchardLogic.Offset offset = entry.getKey();
            BlockState state = leaves.defaultBlockState()
                    .setValue(FruitLeavesBlock.DISTANCE, entry.getValue())
                    .setValue(FruitLeavesBlock.PERSISTENT, false)
                    .setValue(FruitLeavesBlock.STAGE, 0);
            level.setBlock(origin.offset(offset.x(), offset.y(), offset.z()), state, Block.UPDATE_CLIENTS);
        }
        return true;
    }

    private static boolean isFree(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
}
