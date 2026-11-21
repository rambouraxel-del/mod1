package fr.terresvivantes.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Arroseur (cuivre ou fer). Petit bloc socle + tube + buse, non gorgeable d'eau. Son BlockEntity
 * tourne côté serveur toutes les {@link fr.terresvivantes.water.logic.WaterLogic#SPRINKLER_TICKS} ticks.
 */
public final class SprinklerBlock extends Block implements EntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /** Forme : socle 8x2x8 en bas, tube 2x6x2 au centre, buse 4x2x4 en haut (emprise 6x10x6). */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5, 0, 5, 11, 2, 11),
            Block.box(7, 2, 7, 9, 8, 9),
            Block.box(6, 8, 6, 10, 10, 10));

    private final int radius;

    public SprinklerBlock(BlockBehaviour.Properties properties, int radius) {
        super(properties);
        this.radius = radius;
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    /** Rayon du carré arrosé, en blocs depuis l'arroseur. */
    public int radius() {
        return radius;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SprinklerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || type != WaterModule.SPRINKLER_BE.get()) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<SprinklerBlockEntity>) (lvl, pos, st, be) ->
                be.serverTick((ServerLevel) lvl, pos, st);
    }

    /** Client : gouttes en arc et, rarement, un léger bruit d'eau. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.5;
            // Vitesse initiale vers le haut et vers l'extérieur : la particule retombe en arc.
            double outward = random.nextDouble() * 2 - 1;
            double sideways = random.nextDouble() * 2 - 1;
            level.addParticle(ParticleTypes.SPLASH, x, y, z, outward * 0.06, 0.22, sideways * 0.06);
        }
        if (random.nextInt(240) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.15f, 1.6f + random.nextFloat() * 0.2f, false);
        }
    }
}
