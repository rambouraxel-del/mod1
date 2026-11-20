package fr.terresvivantes.preserving;

import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.preserving.logic.PreservingLogic;
import fr.terresvivantes.preserving.logic.SpeedCarry;
import fr.terresvivantes.processing.ProcessingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity du tonneau : la température locale ralentit (trop chaud) ou accélère (cave fraîche) la fermentation.
 * Le facteur est appliqué à chaque tick de progression par {@link SpeedCarry}, sans aléatoire.
 */
public final class FermentationBarrelBlockEntity extends ProcessingBlockEntity {

    private final SpeedCarry carry = new SpeedCarry();

    public FermentationBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(PreservingModule.FERMENTATION_BARREL_BE.get(), pos, state, PreservingModule.BARREL_TYPE);
    }

    @Override
    protected void craftingTick(ServerLevel level, BlockPos pos) {
        float factor = PreservingLogic.fermentationFactor(ClimateApi.temperatureAt(level, pos));
        PreservingSpeed.adjust(this, carry.step(factor));
        if (level.getGameTime() % 40 == 0) {
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.3F, 1.0F);
        }
    }
}
