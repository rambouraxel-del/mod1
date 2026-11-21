package fr.terresvivantes.compost;

import fr.terresvivantes.compost.logic.CompostLogic;
import fr.terresvivantes.soil.SoilApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Fertilisant : clic droit sur une terre labouree applique ses effets au bloc cible seul. */
public final class FertilizerItem extends Item {

    private final CompostLogic.Fertilizer fertilizer;

    public FertilizerItem(Properties properties, CompostLogic.Fertilizer fertilizer) {
        super(properties);
        this.fertilizer = fertilizer;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            SoilApi.fertilize(level, pos, fertilizer.nitrogen(), fertilizer.phosphorus(),
                    fertilizer.potassium(), fertilizer.organic());
            feedback(level, pos);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Son et particules verts sur la terre fertilisee. Appele cote serveur. */
    static void feedback(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    10, 0.3, 0.2, 0.3, 0.0);
        }
    }
}
