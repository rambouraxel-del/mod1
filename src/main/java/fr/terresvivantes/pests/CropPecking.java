package fr.terresvivantes.pests;

import fr.terresvivantes.pests.logic.CrowLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Application d'un coup de bec sur une culture. Une culture protégée par un épouvantail n'est pas touchée.
 * Le tirage est passé en paramètre pour que les tests de jeu soient déterministes.
 */
public final class CropPecking {

    private CropPecking() {
    }

    /**
     * Applique un coup de bec à la culture en {@code pos}. {@code roll} est un tirage dans [0, 1). Renvoie l'issue
     * (NONE si ce n'est pas une culture, si elle est protégée, ou si le tirage échoue).
     */
    public static CrowLogic.PeckOutcome peck(ServerLevel level, BlockPos pos, double roll) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop)) {
            return CrowLogic.PeckOutcome.NONE;
        }
        if (PestsApi.protectsCrop(level, pos)) {
            return CrowLogic.PeckOutcome.NONE;
        }
        int age = crop.getAge(state);
        CrowLogic.PeckOutcome outcome = CrowLogic.peckOutcome(age, roll, PestsConfig.peckChance());
        switch (outcome) {
            case REGRESS -> level.setBlock(pos, crop.getStateForAge(CrowLogic.regressedAge(age)), Block.UPDATE_ALL);
            case DESTROY -> level.destroyBlock(pos, false);
            case NONE -> {
            }
        }
        return outcome;
    }
}
