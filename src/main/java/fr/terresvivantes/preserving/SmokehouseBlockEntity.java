package fr.terresvivantes.preserving;

import fr.terresvivantes.processing.ProcessingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** BlockEntity du fumoir : panache de fumée de camp au-dessus pendant la fumaison. */
public final class SmokehouseBlockEntity extends ProcessingBlockEntity {

    public SmokehouseBlockEntity(BlockPos pos, BlockState state) {
        super(PreservingModule.SMOKEHOUSE_BE.get(), pos, state, PreservingModule.SMOKEHOUSE_TYPE);
    }

    @Override
    protected void craftingTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 4 == 0) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0,
                    pos.getZ() + 0.5, 1, 0.1, 0.0, 0.1, 0.01);
        }
    }
}
