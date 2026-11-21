package fr.terresvivantes.water;

import fr.terresvivantes.water.logic.WaterLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Tonneau de pluie. Niveau 0 à 8 (LEVEL). Se remplit par randomTick sous la pluie ciel ouvert,
 * et par seau d'eau (niveau 8), fiole (-1 niveau, bouteille d'eau) et seau vide (sur niveau 8, donne de l'eau).
 * Comparateur : signal = niveau.
 */
public final class RainBarrelBlock extends Block {

    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;

    public RainBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos.above())) {
            int current = state.getValue(LEVEL);
            int next = WaterLogic.afterRain(current);
            if (next != current) {
                level.setBlock(pos, state.setValue(LEVEL, next), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        int current = state.getValue(LEVEL);
        if (stack.is(Items.WATER_BUCKET)) {
            if (current == WaterLogic.BARREL_MAX_LEVEL) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LEVEL, WaterLogic.BARREL_MAX_LEVEL), Block.UPDATE_ALL);
                replaceHeld(player, hand, stack, new ItemStack(Items.BUCKET));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.is(Items.BUCKET)) {
            if (current != WaterLogic.BARREL_MAX_LEVEL) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LEVEL, 0), Block.UPDATE_ALL);
                replaceHeld(player, hand, stack, new ItemStack(Items.WATER_BUCKET));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (!WaterLogic.canDraw(current, 1)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(LEVEL, current - 1), Block.UPDATE_ALL);
                ItemStack bottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
                replaceHeld(player, hand, stack, bottle);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** Remplace l'objet tenu par result (un seul objet : remplacement direct, plusieurs : ajout à l'inventaire). */
    private static void replaceHeld(Player player, InteractionHand hand, ItemStack held, ItemStack result) {
        if (player.getAbilities().instabuild) {
            return;
        }
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }
}
