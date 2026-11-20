package fr.terresvivantes.compost;

import fr.terresvivantes.compost.logic.CompostLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bac a compost en bois. Niveau 0 a 8 (LEVEL) et etat pret (READY). Pas de BlockEntity :
 * la maturation se fait par randomTick.
 */
public final class CompostBinBlock extends Block {

    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;
    public static final BooleanProperty READY = BooleanProperty.create("ready");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CompostBinBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LEVEL, 0)
                .setValue(READY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEVEL, READY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Clic avec une matiere acceptee : remplit le bac. Sinon, laisse la main vide traiter le clic. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        int added = CompostLogic.levelsFor(stack.is(CompostModule.MANURE.get()),
                stack.is(CompostModule.COMPOSTABLE));
        int current = state.getValue(LEVEL);
        if (added == 0 || state.getValue(READY) || current >= CompostLogic.MAX_LEVEL) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(LEVEL, CompostLogic.addLevels(current, added)), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Clic main vide sur un bac pret : recolte 2 a 3 compost et vide le bac. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!state.getValue(READY)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int amount = CompostLogic.harvestAmount(level.getRandom().nextDouble());
            ItemStack compost = new ItemStack(CompostModule.COMPOST.get(), amount);
            player.getInventory().add(compost);
            if (!compost.isEmpty()) {
                player.drop(compost, false);
            }
            level.setBlock(pos, state.setValue(LEVEL, 0).setValue(READY, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Un bac plein peut maturer : passe en READY selon la probabilite par randomTick. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (CompostLogic.shouldMature(state.getValue(LEVEL), state.getValue(READY), random.nextDouble())) {
            level.setBlock(pos, state.setValue(READY, true), Block.UPDATE_ALL);
        }
    }
}
