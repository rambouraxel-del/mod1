package fr.terresvivantes.rice;

import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.crop.logic.GrowthDecision;
import fr.terresvivantes.crop.logic.GrowthModel;
import fr.terresvivantes.rice.logic.RiceLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Riz : culture aquatique. Le plant remplace une source d'eau de 1 bloc de profondeur au-dessus d'une terre
 * (dirt, grass, farmland, mud) et reste waterlogged. Il survit tant que le sol et l'eau voisine sont là
 * ({@link RiceLogic#survives}); sinon il casse à la prochaine tick (drops : plant de riz).
 * Croît par randomTick, sans BlockEntity, avec les règles de {@link GrowthModel}.
 */
public final class RiceCropBlock extends CropBlock implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final Supplier<? extends ItemLike> seedling;

    public RiceCropBlock(Supplier<? extends ItemLike> seedling, BlockBehaviour.Properties properties) {
        super(properties);
        this.seedling = seedling;
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, true));
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seedling.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    /** Sol admis pour une rizière : terre, herbe, terre labourée ou boue. */
    public static boolean soilAllows(BlockState soil) {
        return soil.is(Blocks.DIRT) || soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.FARMLAND) || soil.is(Blocks.MUD);
    }

    /** Nombre de voisins horizontaux contenant de l'eau. */
    public static int waterNeighbours(BlockGetter level, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                count++;
            }
        }
        return count;
    }

    /** Vrai si le bloc à {@code pos} peut porter un plant de riz (sol admis, eau, voisins). */
    public static boolean canGrowAt(BlockGetter level, BlockPos pos, boolean waterlogged) {
        return RiceLogic.survives(soilAllows(level.getBlockState(pos.below())), waterlogged,
                waterNeighbours(level, pos));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canGrowAt(level, pos, state.getValue(WATERLOGGED));
    }

    /**
     * Ne planifie qu'une vérification : la casse se fait à la tick suivante, pour ne pas modifier le monde
     * pendant une mise à jour de voisinage.
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Le riz ne se remplit ni ne se vide au seau : l'eau de la rizière est fixée par la plantation. */
    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < RiceLogic.MIN_GROWTH_LIGHT) {
            return;
        }
        int age = getAge(state);
        if (age >= getMaxAge() || !canSurvive(state, level, pos)) {
            return;
        }
        if (random.nextDouble() >= RiceLogic.attemptChance()) {
            return;
        }
        float factor = RiceLogic.growthFactor(
                SeasonApi.seasonGrowthMultiplier(level, pos),
                SeasonApi.getSeason(level),
                ClimateApi.temperatureAt(level, pos));
        GrowthDecision decision = GrowthModel.decide(factor, random.nextDouble(), random.nextDouble());
        if (decision == GrowthDecision.DENY) {
            return;
        }
        int newAge = RiceLogic.clampAge(age + (decision == GrowthDecision.EXTRA_STAGE ? 2 : 1));
        level.setBlock(pos, state.setValue(AGE, newAge), Block.UPDATE_CLIENTS);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.0);
    }
}
