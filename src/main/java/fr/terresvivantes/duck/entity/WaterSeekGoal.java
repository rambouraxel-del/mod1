package fr.terresvivantes.duck.entity;

import fr.terresvivantes.duck.logic.DuckLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Le canard préfère l'eau : hors de l'eau, il tire quelques positions proches et se dirige vers la première surface
 * d'eau trouvée (la navigation du canard passe sur l'eau). Déclenchée au hasard, pas à chaque tick.
 */
final class WaterSeekGoal extends Goal {

    private final Duck duck;
    private final double speed;
    private BlockPos target;

    WaterSeekGoal(Duck duck, double speed) {
        this.duck = duck;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (duck.isInWater() || duck.isBaby() || duck.getRandom().nextInt(DuckLogic.WATER_SEEK_CHANCE) != 0) {
            return false;
        }
        target = findWater(duck.level(), duck.getRandom());
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && !duck.isInWater() && !duck.getNavigation().isDone();
    }

    @Override
    public void start() {
        duck.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
    }

    @Override
    public void stop() {
        target = null;
        duck.getNavigation().stop();
    }

    private BlockPos findWater(Level level, RandomSource random) {
        BlockPos origin = duck.blockPosition();
        int radius = DuckLogic.WATER_SEEK_RADIUS;
        for (int i = 0; i < DuckLogic.WATER_SEEK_TRIES; i++) {
            BlockPos candidate = origin.offset(
                    random.nextInt(2 * radius + 1) - radius,
                    random.nextInt(5) - 2,
                    random.nextInt(2 * radius + 1) - radius);
            if (level.getFluidState(candidate).is(FluidTags.WATER) && level.getFluidState(candidate.above()).isEmpty()) {
                return candidate;
            }
        }
        return null;
    }
}
