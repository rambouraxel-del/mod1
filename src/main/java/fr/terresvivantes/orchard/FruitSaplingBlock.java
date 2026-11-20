package fr.terresvivantes.orchard;

import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.OrchardLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Pousse fruitière. Pas de stade intermédiaire : le sapling devient directement un petit arbre.
 * La croissance par randomTick dépend de la saison (nulle en hiver) ; le bonemeal force l'arbre.
 * Tient sur les mêmes terres que les saplings vanilla ; se casse si la terre disparaît.
 */
public final class FruitSaplingBlock extends Block implements BonemealableBlock {

    private static final float BONEMEAL_SUCCESS_CHANCE = 0.45f;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    private final FruitTreeDefinition def;
    private final Supplier<FruitLeavesBlock> leaves;

    public FruitSaplingBlock(FruitTreeDefinition def, Supplier<FruitLeavesBlock> leaves,
                             BlockBehaviour.Properties properties) {
        super(properties);
        this.def = def;
        this.leaves = leaves;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return OrchardTrees.isGround(level.getBlockState(pos.below()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) < OrchardLogic.MIN_LIGHT) {
            return;
        }
        float seasonFactor = SeasonApi.seasonGrowthMultiplier(level, pos);
        if (OrchardLogic.saplingGrows(random.nextDouble(), OrchardConfig.saplingGrowthChance(), seasonFactor)) {
            OrchardTrees.grow(level, pos, def, leaves.get(), random);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONEMEAL_SUCCESS_CHANCE;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        OrchardTrees.grow(level, pos, def, leaves.get(), random);
    }
}
