package fr.terresvivantes.preserving;

import fr.terresvivantes.preserving.logic.PreservingLogic;
import fr.terresvivantes.preserving.logic.SpeedCarry;
import fr.terresvivantes.processing.ProcessingBlock;
import fr.terresvivantes.processing.ProcessingBlockEntity;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity du séchoir. Il n'avance que s'il voit le ciel et qu'il ne pleut pas sur lui; la saison accélère (été)
 * ou ralentit (hiver) le séchage.
 */
public final class DryingRackBlockEntity extends ProcessingBlockEntity {

    private final SpeedCarry carry = new SpeedCarry();

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(PreservingModule.DRYING_RACK_BE.get(), pos, state, PreservingModule.DRYING_RACK_TYPE);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (inventory().getStackInSlot(0).isEmpty()) {
            super.serverTick(level, pos, state);
            return;
        }
        BlockPos above = pos.above();
        // Pluie globale du niveau (comme VetNeeds) : évite qu'une biome sans précipitations rende le test vide.
        boolean dry = PreservingLogic.canDry(level.canSeeSky(above), level.isRaining());
        if (!dry) {
            if (state.getValue(ProcessingBlock.ACTIVE)) {
                level.setBlock(pos, state.setValue(ProcessingBlock.ACTIVE, false), Block.UPDATE_ALL);
            }
            return;
        }
        super.serverTick(level, pos, state);
    }

    @Override
    protected void craftingTick(ServerLevel level, BlockPos pos) {
        float factor = PreservingLogic.dryingSeasonFactor(SeasonApi.getSeason(level));
        PreservingSpeed.adjust(this, carry.step(factor));
    }
}
