package fr.terresvivantes.crop;

import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropLogic;
import fr.terresvivantes.crop.logic.GrowthDecision;
import fr.terresvivantes.crop.logic.GrowthModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Culture du mod. Croît par randomTick (pas de BlockEntity). Chaque tentative passe par
 * {@link GrowthModel#decide}; chaque stade gagné consomme le sol.
 */
public final class TVCropBlock extends CropBlock {

    private final CropDefinition definition;
    private final Supplier<? extends ItemLike> seeds;

    public TVCropBlock(CropDefinition definition, Supplier<? extends ItemLike> seeds, BlockBehaviour.Properties properties) {
        super(properties);
        this.definition = definition;
        this.seeds = seeds;
    }

    public CropDefinition definition() {
        return definition;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seeds.get();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < CropLogic.MIN_GROWTH_LIGHT) {
            return;
        }
        int age = getAge(state);
        if (age >= getMaxAge()) {
            return;
        }
        if (random.nextDouble() >= CropLogic.attemptChance(definition.growthDays())) {
            return;
        }
        float factor = CropGrowth.tvFactor(level, pos, definition);
        GrowthDecision decision = GrowthModel.decide(factor, random.nextDouble(), random.nextDouble());
        if (decision == GrowthDecision.DENY) {
            return;
        }
        int newAge = CropLogic.clampAge(age + (decision == GrowthDecision.EXTRA_STAGE ? 2 : 1));
        for (int gained = age; gained < newAge; gained++) {
            CropGrowth.consumeStage(level, pos, definition);
        }
        level.setBlock(pos, getStateForAge(newAge), Block.UPDATE_CLIENTS);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.0);
    }
}
