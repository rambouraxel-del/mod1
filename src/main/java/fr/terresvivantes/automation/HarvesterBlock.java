package fr.terresvivantes.automation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Récolteuse : bloc orienté qui récolte les cultures mûres devant elle. */
public final class HarvesterBlock extends AutomationBlock {

    public static final MapCodec<HarvesterBlock> CODEC = simpleCodec(HarvesterBlock::new);

    public HarvesterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<HarvesterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HarvesterBlockEntity(pos, state);
    }
}
