package fr.terresvivantes.dairy.block;

import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.processing.ProcessingBlockEntity;
import fr.terresvivantes.processing.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/** BlockEntity de la baratte : clapotis pendant le barattage, seau rendu après la crème. */
public final class ChurnBlockEntity extends ProcessingBlockEntity {

    public ChurnBlockEntity(BlockPos pos, BlockState state) {
        super(DairyModule.CHURN_BE.get(), pos, state, DairyModule.CHURN_TYPE);
    }

    @Override
    protected void craftingTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_STEP, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
    }

    @Override
    protected void craftFinished(ServerLevel level, BlockPos pos, ProcessingRecipe finished) {
        // Seul le lait se transforme en crème avec un seau à rendre; la crème en beurre ne rend rien.
        if (finished.result().is(DairyModule.CREAM.get())) {
            DairyModule.ejectBucket(level, pos);
        }
    }
}
