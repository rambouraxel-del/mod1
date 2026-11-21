package fr.terresvivantes.rice;

import fr.terresvivantes.rice.logic.RiceLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;

/** Façade publique du module rice. */
public final class RiceApi {

    private RiceApi() {
    }

    /**
     * Plante du riz au-dessus d'une terre admise, sur une source d'eau de 1 bloc de profondeur.
     * Le bloc d'eau visé est remplacé par la culture waterlogged. Retourne {@code false} si les conditions ne sont pas réunies.
     *
     * @param soil position de la terre (le plant est en {@code soil.above()})
     */
    public static boolean plant(ServerLevel level, BlockPos soil) {
        BlockPos target = soil.above();
        if (!level.getBlockState(target).is(Blocks.WATER)) {
            return false;
        }
        FluidState fluid = level.getFluidState(target);
        boolean source = fluid.is(FluidTags.WATER) && fluid.isSource();
        boolean waterAbove = level.getFluidState(target.above()).is(FluidTags.WATER);
        boolean soilOk = RiceCropBlock.soilAllows(level.getBlockState(soil));
        int neighbours = RiceCropBlock.waterNeighbours(level, target);
        if (!RiceLogic.canPlant(soilOk, source, waterAbove, neighbours)) {
            return false;
        }
        level.setBlock(target, RiceModule.RICE_CROP.get().defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }
}
