package fr.terresvivantes.milling;

import fr.terresvivantes.processing.ProcessingBlockEntity;
import fr.terresvivantes.processing.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/** BlockEntity du pressoir : bruit de piston pendant le pressage, éclaboussure de jus à la fin. */
public final class FruitPressBlockEntity extends ProcessingBlockEntity {

    public FruitPressBlockEntity(BlockPos pos, BlockState state) {
        super(MillingModule.FRUIT_PRESS_BE.get(), pos, state, MillingModule.FRUIT_PRESS_TYPE);
    }

    @Override
    protected void craftingTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.4F, 0.8F);
        }
    }

    @Override
    protected void craftFinished(ServerLevel level, BlockPos pos, ProcessingRecipe finished) {
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                10, 0.3, 0.1, 0.3, 0.05);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.0F);
    }
}
