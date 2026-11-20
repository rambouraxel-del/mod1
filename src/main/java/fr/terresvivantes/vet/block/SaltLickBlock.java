package fr.terresvivantes.vet.block;

import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.vet.logic.VetLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/**
 * Bloc de sel préventif. Quand des animaux d'élevage sont proches, chaque randomTick l'use d'un cran (USES 0 à 3);
 * au quatrième usage il disparaît. Son effet sur les animaux est dans le module vet.
 */
public final class SaltLickBlock extends Block {

    public static final IntegerProperty USES = IntegerProperty.create("uses", 0, VetLogic.SALT_LICK_MAX_USES - 1);

    public SaltLickBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(USES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(USES);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AABB area = new AABB(pos).inflate(VetLogic.SALT_LICK_RADIUS);
        if (level.getEntitiesOfClass(Animal.class, area, LivestockApi::isLivestock).isEmpty()) {
            return;
        }
        int after = VetLogic.saltLickAfterUse(state.getValue(USES));
        if (VetLogic.saltLickGone(after)) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(USES, after), Block.UPDATE_CLIENTS);
        }
    }
}
