package fr.terresvivantes.flora;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fleur sauvage du module flora : fleur vanilla (effet de ragoût suspect, tags fleurs, pot) qui tente de se
 * propager par randomTick, voir {@link FloraSpread}.
 */
public final class WildFlowerBlock extends FlowerBlock {

    private final String id;

    public WildFlowerBlock(String id, Holder<MobEffect> stewEffect, float stewDurationSeconds,
                           BlockBehaviour.Properties properties) {
        super(stewEffect, stewDurationSeconds, properties);
        this.id = id;
    }

    /** Identifiant court (ex. {@code clover}). */
    public String id() {
        return id;
    }

    /** Identifiant de ressource du bloc, ex. {@code terresvivantes:clover}. */
    public ResourceLocation resourceId() {
        return ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, id);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FloraSpread.attempt(level, pos, this, random, FloraConfig.spreadChance());
    }
}
