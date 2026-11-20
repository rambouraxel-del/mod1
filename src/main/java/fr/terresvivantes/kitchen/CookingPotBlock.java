package fr.terresvivantes.kitchen;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Marmite : bloc de la machine de cuisine (4 entrées, un bol en conteneur). Son volume est celui d'un chaudron réduit
 * (12 x 10 x 12 pixels), pas un cube plein; la forme sert au rendu, à la collision et au clic.
 */
public final class CookingPotBlock extends ProcessingBlock {

    public static final MapCodec<CookingPotBlock> CODEC = BlockBehaviour.simpleCodec(CookingPotBlock::new);

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public CookingPotBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CookingPotBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return SHAPE;
    }
}
