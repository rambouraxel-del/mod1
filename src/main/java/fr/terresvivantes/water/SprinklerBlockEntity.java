package fr.terresvivantes.water;

import fr.terresvivantes.water.logic.WaterLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

/**
 * BlockEntity de l'arroseur. Cycle serveur toutes les 100 ticks : alimenté (eau dessous ou à côté, ou tonneau
 * voisin non vide) donne ACTIVE=true et met l'humidité des terres labourées du carré à 7, au même y et y-1.
 * Un tonneau voisin fournit un niveau tous les 4 cycles.
 */
public final class SprinklerBlockEntity extends BlockEntity {

    /** Ticks depuis le dernier cycle. Non sauvegardé : un redémarrage reprend au prochain cycle. */
    private int ticks;
    /** Compteur de cycles alimentés par un tonneau (pour le prélèvement tous les 4 cycles). */
    private int barrelCycle;

    public SprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(WaterModule.SPRINKLER_BE.get(), pos, state);
    }

    /** Appelé par le ticker serveur à chaque tick. Ne fait rien avant la fin de la période. */
    void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (++ticks < WaterLogic.SPRINKLER_TICKS) {
            return;
        }
        ticks = 0;

        boolean fed;
        if (hasWaterSupply(level, pos)) {
            barrelCycle = 0;
            fed = true;
        } else {
            BlockPos barrel = findBarrelWithWater(level, pos);
            if (barrel == null) {
                barrelCycle = 0;
                fed = false;
            } else {
                fed = !WaterLogic.drawsFromBarrel(barrelCycle) || WaterApi.tryDrawWater(level, barrel, 1);
                barrelCycle++;
            }
        }

        if (state.getValue(SprinklerBlock.ACTIVE) != fed) {
            level.setBlock(pos, state.setValue(SprinklerBlock.ACTIVE, fed), Block.UPDATE_ALL);
        }
        if (fed) {
            wetSquare(level, pos, ((SprinklerBlock) state.getBlock()).radius());
        }
    }

    /** Eau directement dessous ou adjacente horizontalement. */
    private static boolean hasWaterSupply(ServerLevel level, BlockPos pos) {
        if (level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /** Tonneau de pluie voisin (6 faces) contenant au moins un niveau d'eau. */
    private static BlockPos findBarrelWithWater(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            BlockState state = level.getBlockState(neighbour);
            if (WaterApi.isRainBarrel(state) && state.getValue(RainBarrelBlock.LEVEL) > 0) {
                return neighbour;
            }
        }
        return null;
    }

    /** Met la terre labourée du carré à l'humidité maximale, au niveau de l'arroseur et en dessous. */
    private static void wetSquare(ServerLevel level, BlockPos pos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy >= -1; dy--) {
                    BlockPos target = pos.offset(dx, dy, dz);
                    BlockState soil = level.getBlockState(target);
                    if (soil.is(Blocks.FARMLAND)) {
                        level.setBlock(target, soil.setValue(FarmBlock.MOISTURE, WaterLogic.WET_MOISTURE),
                                Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
