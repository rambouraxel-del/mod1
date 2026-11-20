package fr.terresvivantes.pests.entity;

import fr.terresvivantes.pests.CropPecking;
import fr.terresvivantes.pests.PestsApi;
import fr.terresvivantes.pests.PestsConfig;
import fr.terresvivantes.pests.logic.CrowLogic;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Cherche une culture mûre dans {@link CrowLogic#SEARCH_RADIUS} blocs (hors zone d'épouvantail), s'y pose et picore
 * tous les {@link CrowLogic#PECK_INTERVAL_TICKS} ticks. Actif seulement de jour, hors hiver.
 */
final class PeckCropGoal extends Goal {

    private static final double ARRIVED_DISTANCE_SQ = 2.25;

    private final Crow crow;
    @Nullable
    private BlockPos target;
    private int perchTicks;

    PeckCropGoal(Crow crow) {
        this.crow = crow;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(crow.level() instanceof ServerLevel level) || !PestsConfig.crowsEnabled()) {
            return false;
        }
        boolean winter = SeasonApi.getSeason(level) == Season.WINTER;
        if (!CrowLogic.mayPeck(level.isDay(), winter)) {
            return false;
        }
        if (crow.getRandom().nextInt(CrowLogic.SEARCH_INTERVAL_TICKS) != 0) {
            return false;
        }
        target = findTarget(level, crow.blockPosition());
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (target == null || !(crow.level() instanceof ServerLevel level)) {
            return false;
        }
        return CrowLogic.isTargetCrop(cropAge(level, target))
                && !CrowLogic.mustLeave(perchTicks)
                && !PestsApi.protectsCrop(level, target);
    }

    @Override
    public void start() {
        perchTicks = 0;
    }

    @Override
    public void tick() {
        if (target == null || !(crow.level() instanceof ServerLevel level)) {
            return;
        }
        double cx = target.getX() + 0.5;
        double cy = target.getY();
        double cz = target.getZ() + 0.5;
        if (!crow.isPecking()) {
            if (crow.position().distanceToSqr(cx, cy, cz) > ARRIVED_DISTANCE_SQ) {
                if (crow.getNavigation().isDone() || crow.tickCount % 10 == 0) {
                    crow.getNavigation().moveTo(cx, cy, cz, 1.0D);
                }
                return;
            }
            crow.startPecking(target);
        }
        crow.perchOn(target);
        perchTicks++;
        if (CrowLogic.isPeckTick(perchTicks)) {
            peck(level);
        }
    }

    @Override
    public void stop() {
        crow.stopPecking();
        crow.getNavigation().stop();
        target = null;
        perchTicks = 0;
    }

    private void peck(ServerLevel level) {
        CropPecking.peck(level, target, crow.getRandom().nextDouble());
        Vec3 at = Vec3.atCenterOf(target).add(0.0, 0.3, 0.0);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 3, 0.1, 0.1, 0.1, 0.02);
        crow.playSound(SoundEvents.PARROT_AMBIENT, 0.8F, 0.5F);
    }

    /** Culture cible la plus proche du corbeau, hors zone d'épouvantail, ou null. */
    @Nullable
    static BlockPos findTarget(ServerLevel level, BlockPos center) {
        int radius = CrowLogic.SEARCH_RADIUS;
        int height = CrowLogic.SEARCH_HEIGHT;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -height, -radius),
                center.offset(radius, height, radius))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop) || !CrowLogic.isTargetCrop(crop.getAge(state))) {
                continue;
            }
            BlockPos soil = pos.below();
            if (!level.getBlockState(soil).isFaceSturdy(level, soil, Direction.UP)) {
                continue;
            }
            if (PestsApi.protectsCrop(level, pos)) {
                continue;
            }
            double distance = pos.distSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    /** Âge de la culture en {@code pos}, ou -1 si ce n'est pas une culture. */
    private static int cropAge(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : -1;
    }
}
