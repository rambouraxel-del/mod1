package fr.terresvivantes.storage;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.storage.logic.CrateLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Caisse : bloc orienté (face avant = {@link #FACING}), contenu dans {@link CrateBlockEntity}. */
public final class CrateBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<CrateBlock> CODEC = BlockBehaviour.simpleCodec(CrateBlock::new);

    public CrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    /** Pose : le contenu porté par l'item de caisse est repris. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            crate.readFrom(stack);
        }
    }

    /** Clic droit avec un item : insère le stack en main ; double clic rapide : insère aussi les stacks identiques. */
    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean doubleClick = crate.registerClick(player, level.getGameTime());
        ItemStack reference = stack.copy();
        int moved = crate.insertFrom(stack);
        if (doubleClick) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack other = player.getInventory().getItem(slot);
                if (other != stack && !other.isEmpty() && ItemStack.isSameItemSameComponents(other, reference)) {
                    moved += crate.insertFrom(other);
                }
            }
        }
        if (moved <= 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        player.getInventory().setChanged();
        return ItemInteractionResult.SUCCESS;
    }

    /** Clic droit à main vide : résumé du contenu dans la barre d'action. */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            player.displayClientMessage(summary(crate), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Clic gauche : retire une stack (un seul item accroupi) et la donne au joueur. */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
            return;
        }
        int take = CrateLogic.attackTake(crate.count(), player.isShiftKeyDown(), crate.maxStack());
        if (take <= 0) {
            return;
        }
        ItemStack out = crate.extract(take);
        if (!out.isEmpty()) {
            player.getInventory().placeItemBackInInventory(out);
        }
    }

    /** Casse, explosion ou remplacement : la caisse tombe avec son contenu dans le composant de l'item. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel
                && level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            ItemStack drop = new ItemStack(StorageModule.CRATE_ITEM.get());
            crate.writeTo(drop);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparateur : niveau selon le remplissage de la caisse. */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
            return CrateLogic.comparatorLevel(crate.count(), crate.capacity());
        }
        return 0;
    }

    /** Résumé de la caisse : « x 312 Tomate » ou « Caisse vide ». */
    static Component summary(CrateBlockEntity crate) {
        if (crate.item() == null || crate.count() <= 0) {
            return Component.translatable("message.terresvivantes.crate_empty");
        }
        return CrateItem.contentsText(new CrateContents(crate.item(), crate.count()));
    }
}
