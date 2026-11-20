package fr.terresvivantes.milling;

import fr.terresvivantes.processing.ProcessingBlockEntity;
import fr.terresvivantes.processing.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/** BlockEntity de la meule : grincement toutes les secondes pendant la mouture, nuage de farine à la fin. */
public final class MillstoneBlockEntity extends ProcessingBlockEntity {

    public MillstoneBlockEntity(BlockPos pos, BlockState state) {
        super(MillingModule.MILLSTONE_BE.get(), pos, state, MillingModule.MILLSTONE_TYPE);
    }

    @Override
    protected void craftingTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }

    @Override
    protected void craftFinished(ServerLevel level, BlockPos pos, ProcessingRecipe finished) {
        level.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.3, 0.1, 0.3, 0.02);
    }
}
