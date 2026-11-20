package fr.terresvivantes.storage;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.storage.logic.SiloLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Silo : bloc vertical empilable (1 à 8). Une colonne forme un stockage partagé, porté par son bloc du bas.
 */
public final class SiloBlock extends Block implements EntityBlock {

    public static final MapCodec<SiloBlock> CODEC = BlockBehaviour.simpleCodec(SiloBlock::new);

    public SiloBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SiloBlockEntity(pos, state);
    }

    /** Refuse la pose si la colonne dépasserait 8 blocs. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!SiloLogic.canJoin(run(level, pos, Direction.DOWN), run(level, pos, Direction.UP))) {
            return null;
        }
        return defaultBlockState();
    }

    /**
     * Pose sous une colonne : le nouveau bloc devient le bas, et le contenu de l'ancien bas lui est transféré.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || level.getBlockState(pos.below()).is(this) || !level.getBlockState(pos.above()).is(this)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof SiloBlockEntity bottom
                && level.getBlockEntity(pos.above()) instanceof SiloBlockEntity oldBottom) {
            bottom.takeStacksFrom(oldBottom);
        }
    }

    /** Clic droit : résumé de la colonne dans la barre d'action. */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SiloBlockEntity silo) {
            player.displayClientMessage(summary(silo), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Casse du bloc du bas qui contient des items : le joueur est prévenu que le contenu tombe au sol. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SiloBlockEntity silo
                && silo.isController() && silo.totalItems() > 0) {
            player.displayClientMessage(Component.translatable("message.terresvivantes.silo_dropped",
                    silo.totalItems()), true);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Casse ou remplacement : le contenu du contrôleur tombe au sol. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel
                && level.getBlockEntity(pos) instanceof SiloBlockEntity silo) {
            silo.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Nombre de blocs de silo contigus dans une direction (borné à 8). */
    private static int run(Level level, BlockPos pos, Direction direction) {
        int count = 0;
        BlockPos cursor = pos.relative(direction);
        while (count < SiloLogic.MAX_HEIGHT && level.getBlockState(cursor).is(StorageModule.SILO.get())) {
            count++;
            cursor = cursor.relative(direction);
        }
        return count;
    }

    /** Résumé : types, total et remplissage de la colonne. */
    static Component summary(SiloBlockEntity silo) {
        SiloBlockEntity controller = silo.controller();
        int types = 0;
        java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
        for (ItemStack stack : controller.contents()) {
            if (seen.add(stack.getItem())) {
                types++;
            }
        }
        int used = controller.contents().size();
        return Component.translatable("message.terresvivantes.silo_summary", types, controller.totalItems(),
                SiloLogic.percent(used, controller.capacity()));
    }
}
