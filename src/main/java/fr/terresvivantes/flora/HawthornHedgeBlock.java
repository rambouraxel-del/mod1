package fr.terresvivantes.flora;

import fr.terresvivantes.flora.logic.FloraLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Haie d'aubépine : bloc de type mur feuillu (connexions de mur vers les voisins de la même famille), qui fleurit
 * au printemps. Compte comme habitat pour l'indice de biodiversité.
 */
public final class HawthornHedgeBlock extends WallBlock {

    /** Vrai au printemps, faux le reste de l'année. Mis à jour par randomTick. */
    public static final BooleanProperty FLOWERING = BooleanProperty.create("flowering");

    public HawthornHedgeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FLOWERING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FLOWERING);
    }

    /**
     * Valeur de {@link #FLOWERING} utilisée par {@link WallBlock} pour indexer ses formes : la première valeur de la
     * propriété (celle de {@code any()} au moment de la construction de WallBlock, c'est-à-dire {@code true}).
     */
    private static final boolean SHAPE_FLOWERING = FLOWERING.getPossibleValues().iterator().next();

    /**
     * Les formes de {@link WallBlock} sont précalculées par état sans tenir compte de {@link #FLOWERING} : la
     * géométrie est donc lue sur la valeur de référence de la propriété.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getShape(withShapeFlowering(state), level, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getCollisionShape(withShapeFlowering(state), level, pos, context);
    }

    private static BlockState withShapeFlowering(BlockState state) {
        return state.setValue(FLOWERING, SHAPE_FLOWERING);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean flowering = FloraLogic.hedgeFlowers(SeasonApi.getSeason(level));
        if (state.getValue(FLOWERING) != flowering) {
            level.setBlock(pos, state.setValue(FLOWERING, flowering), Block.UPDATE_CLIENTS);
        }
    }
}
