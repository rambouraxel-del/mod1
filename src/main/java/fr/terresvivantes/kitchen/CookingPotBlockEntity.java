package fr.terresvivantes.kitchen;

import fr.terresvivantes.processing.ProcessingBlock;
import fr.terresvivantes.processing.ProcessingBlockEntity;
import fr.terresvivantes.processing.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * BlockEntity de la marmite. Elle ne progresse que sur une source de chaleur (tag {@link KitchenModule#HEAT_SOURCES}
 * sous elle, allumée si elle a un état {@code lit}). Pendant la cuisson : vapeur, bulles et bouillonnement rare.
 */
public final class CookingPotBlockEntity extends ProcessingBlockEntity {

    /** Vrai si le bloc posé sous la marmite la chauffe : tag de chaleur, et allumé s'il a un état {@code lit}. */
    public static boolean isHeatSource(BlockState state) {
        if (!state.is(KitchenModule.HEAT_SOURCES)) {
            return false;
        }
        return !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT);
    }

    public CookingPotBlockEntity(BlockPos pos, BlockState state) {
        super(KitchenModule.COOKING_POT_BE.get(), pos, state, KitchenModule.COOKING_POT_TYPE);
    }

    /** Sans chaleur dessous, la marmite reste éteinte et sa progression est gelée. */
    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isHeatSource(level.getBlockState(pos.below()))) {
            if (state.hasProperty(ProcessingBlock.ACTIVE) && state.getValue(ProcessingBlock.ACTIVE)) {
                level.setBlock(pos, state.setValue(ProcessingBlock.ACTIVE, false), Block.UPDATE_ALL);
            }
            return;
        }
        super.serverTick(level, pos, state);
    }

    @Override
    protected void craftingTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 8 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    2, 0.25, 0.05, 0.25, 0.01);
        }
        if (level.getGameTime() % 6 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    1, 0.3, 0.05, 0.3, 0.0);
        }
        if (level.random.nextInt(80) == 0) {
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    @Override
    protected void craftFinished(ServerLevel level, BlockPos pos, ProcessingRecipe finished) {
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                10, 0.3, 0.1, 0.3, 0.02);
    }
}
