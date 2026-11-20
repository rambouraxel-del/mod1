package fr.terresvivantes.water;

import fr.terresvivantes.water.logic.WaterLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Facade publique du module water. Les autres modules n'appellent que cette classe.
 */
public final class WaterApi {

    /** Niveaux prélevés dans un tonneau quand l'arrosoir se remplit. */
    public static final int CAN_FILL_LEVELS = WaterLogic.CAN_FILL_LEVELS;

    private WaterApi() {
    }

    /** Vrai si l'état est un tonneau de pluie. */
    public static boolean isRainBarrel(BlockState state) {
        return state.is(WaterModule.RAIN_BARREL.get());
    }

    /**
     * Prélève amount niveaux dans le tonneau à pos. Côté serveur uniquement.
     *
     * @return vrai si le prélèvement a réussi ; faux si ce n'est pas un tonneau ou s'il ne contient pas assez d'eau
     */
    public static boolean tryDrawWater(Level level, BlockPos pos, int amount) {
        if (level.isClientSide()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!isRainBarrel(state)) {
            return false;
        }
        int current = state.getValue(RainBarrelBlock.LEVEL);
        if (!WaterLogic.canDraw(current, amount)) {
            return false;
        }
        level.setBlock(pos, state.setValue(RainBarrelBlock.LEVEL, current - amount), Block.UPDATE_ALL);
        return true;
    }

    /**
     * Vrai si un arroseur actif couvre la terre à soilPos. Un arroseur à la hauteur de la terre ou
     * une case au-dessus la mouille, selon le rayon de son carré.
     */
    public static boolean isCoveredByActiveSprinkler(Level level, BlockPos soilPos) {
        int scan = WaterLogic.IRON_RADIUS;
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -scan; dx <= scan; dx++) {
                for (int dz = -scan; dz <= scan; dz++) {
                    BlockPos candidate = soilPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(candidate);
                    if (state.getBlock() instanceof SprinklerBlock sprinkler
                            && state.getValue(SprinklerBlock.ACTIVE)
                            && WaterLogic.covers(sprinkler.radius(), -dx, -dz)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
