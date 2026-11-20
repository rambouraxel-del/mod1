package fr.terresvivantes.pests.entity;

import fr.terresvivantes.pests.PestsApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Le corbeau qui entre dans la zone d'un épouvantail s'en éloigne. */
final class RepelFromScarecrowGoal extends Goal {

    private static final double FLEE_DISTANCE = 10.0;
    private static final double FLEE_SPEED = 1.4;

    private final Crow crow;

    RepelFromScarecrowGoal(Crow crow) {
        this.crow = crow;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return crow.level() instanceof ServerLevel level
                && PestsApi.coveringScarecrow(level, crow.position()) != null;
    }

    @Override
    public void start() {
        crow.stopPecking();
        if (!(crow.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos scarecrow = PestsApi.coveringScarecrow(level, crow.position());
        if (scarecrow == null) {
            return;
        }
        Vec3 away = new Vec3(crow.getX() - (scarecrow.getX() + 0.5), 0.0, crow.getZ() - (scarecrow.getZ() + 0.5));
        Vec3 direction = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
        Vec3 goal = crow.position().add(direction.scale(FLEE_DISTANCE));
        crow.getNavigation().moveTo(goal.x, crow.getY() + 1.0, goal.z, FLEE_SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        return !crow.getNavigation().isDone();
    }

    @Override
    public void stop() {
        crow.getNavigation().stop();
    }
}
