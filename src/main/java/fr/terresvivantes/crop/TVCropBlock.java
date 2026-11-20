package fr.terresvivantes.crop;

import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropLogic;
import fr.terresvivantes.crop.logic.GrowthDecision;
import fr.terresvivantes.crop.logic.GrowthModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Rendement : le nombre de produits est multiplié par les {@link CropGrowthHooks.YieldModifier} (arrondi
     * probabiliste). Couvre la récolte par clic droit et la casse d'une culture mûre, qui passent toutes deux ici.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        ServerLevel level = params.getLevel();
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        Item product = CropModule.productItem(definition.id());
        List<ItemStack> scaled = new ArrayList<>(drops.size());
        for (ItemStack stack : drops) {
            if (!stack.is(product)) {
                scaled.add(stack);
                continue;
            }
            int count = CropGrowthHooks.scaleYield(level, pos, state, definition, stack.getCount());
            if (count > 0) {
                scaled.add(stack.copyWithCount(count));
            }
        }
        return scaled;
    }

    /** Plantation par un joueur ou une entité : notifie les {@link CropGrowthHooks.PlantListener}. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel server) {
            CropGrowthHooks.notifyPlanted(server, pos, state);
        }
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
        float factor = CropGrowth.tvFactor(level, pos, state, definition);
        GrowthDecision decision = GrowthModel.decide(factor, random.nextDouble(), random.nextDouble());
        CropGrowthHooks.notifyGrowthAttempt(level, pos, state, definition, decision, factor);
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
