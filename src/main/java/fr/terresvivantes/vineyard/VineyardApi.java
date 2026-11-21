package fr.terresvivantes.vineyard;

import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.vineyard.logic.VineyardLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Façade publique du module vineyard. */
public final class VineyardApi {

    private VineyardApi() {
    }

    /** Vrai si la position porte une vigne au stade mûr (grappes prêtes à être récoltées). */
    public static boolean isRipe(BlockState state) {
        return state.getBlock() instanceof GrapevineBlock && state.getValue(GrapevineBlock.STAGE) == VineyardLogic.RIPE_STAGE;
    }

    /**
     * Récolte une vigne mûre : les grappes de son cépage (nombre tiré par {@link VineyardLogic#harvestCount}), la vigne
     * revient au stade 2 et est marquée récoltée. Retourne les grappes à déposer (inventaire du joueur ou de la
     * récolteuse), liste vide si la vigne n'est pas mûre.
     */
    public static List<ItemStack> harvest(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isRipe(state)) {
            return List.of();
        }
        int count = VineyardLogic.harvestCount(level.getRandom().nextDouble());
        ItemStack grapes = new ItemStack(GrapevineBlock.grapesOf(state.getValue(GrapevineBlock.VARIETY)), count);
        QualityModule.setQuality(grapes, VineyardLogic.GRAPE_QUALITY);
        level.setBlock(pos, state.setValue(GrapevineBlock.STAGE, VineyardLogic.MATURE_STAGE)
                .setValue(GrapevineBlock.HARVESTED, true), Block.UPDATE_CLIENTS);
        return List.of(grapes);
    }
}
