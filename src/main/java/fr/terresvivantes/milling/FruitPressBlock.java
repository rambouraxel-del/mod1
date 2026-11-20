package fr.terresvivantes.milling;

import com.mojang.serialization.MapCodec;
import fr.terresvivantes.processing.ProcessingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Pressoir à fruits : machine à une entrée et un conteneur (bouteille de verre). Pendant un pressage, des gouttes de
 * jus tombent du dessus; la couleur suit le fruit quand il est connu, sinon des gouttes de miel.
 */
public final class FruitPressBlock extends ProcessingBlock {

    public static final MapCodec<FruitPressBlock> CODEC = BlockBehaviour.simpleCodec(FruitPressBlock::new);

    /** Couleur du jus par fruit (identifiant de registre), en RGB de 0 à 1. */
    private static final Map<String, Vector3f> JUICE_COLORS = Map.of(
            "minecraft:apple", new Vector3f(0.85f, 0.78f, 0.28f),
            "terresvivantes:peach", new Vector3f(0.98f, 0.66f, 0.42f),
            "terresvivantes:orange", new Vector3f(1.00f, 0.58f, 0.10f));

    public FruitPressBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FruitPressBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(4) != 0) {
            return;
        }
        ParticleOptions drop = juiceParticle(level, pos);
        double x = pos.getX() + 0.35 + random.nextDouble() * 0.3;
        double y = pos.getY() + 0.95;
        double z = pos.getZ() + 0.35 + random.nextDouble() * 0.3;
        level.addParticle(drop, x, y, z, 0.0, 0.0, 0.0);
    }

    private static ParticleOptions juiceParticle(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FruitPressBlockEntity press) {
            ItemStack input = press.inventory().getStackInSlot(0);
            if (!input.isEmpty()) {
                Vector3f color = JUICE_COLORS.get(BuiltInRegistries.ITEM.getKey(input.getItem()).toString());
                if (color != null) {
                    return new DustParticleOptions(color, 1.0f);
                }
            }
        }
        return ParticleTypes.DRIPPING_HONEY;
    }
}
