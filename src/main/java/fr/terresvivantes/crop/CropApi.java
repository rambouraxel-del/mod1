package fr.terresvivantes.crop;

import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Façade publique du module crop. */
public final class CropApi {

    private CropApi() {
    }

    /** Vrai si l'état est une culture à son stade final (cultures du mod et vanilla). */
    public static boolean isMatureCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    /**
     * Récolte une culture mûre, la remet à l'âge 0 et retourne les drops (à déposer au sol par l'appelant).
     * Une culture du mod consomme en plus son sol selon sa récolte. Retourne une liste vide si la culture n'est pas mûre.
     */
    public static List<ItemStack> harvestAndReplant(ServerLevel level, BlockPos pos, Player player) {
        List<ItemStack> drops = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        if (!isMatureCrop(state)) {
            return drops;
        }
        CropBlock crop = (CropBlock) state.getBlock();
        drops.addAll(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, ItemStack.EMPTY));
        if (crop instanceof TVCropBlock tvCrop) {
            CropGrowth.consumeHarvest(level, pos, tvCrop.definition());
        }
        level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
        return drops;
    }

    /** Identifiants des cultures du mod dont la saison est optimale. */
    public static List<String> optimalCropIdsFor(Season season) {
        return CropDefinitions.all().stream()
                .filter(def -> def.optimal().contains(season))
                .map(def -> def.id())
                .toList();
    }

    /**
     * Identifiants des cultures optimales pour la saison ET dont la plage de tolérance
     * ({@code tempMin}..{@code tempMax}) contient la température locale en °C.
     */
    public static List<String> optimalCropIdsFor(Season season, float temperature) {
        return CropDefinitions.all().stream()
                .filter(def -> def.optimal().contains(season))
                .filter(def -> temperature >= def.tempMin() && temperature <= def.tempMax())
                .map(def -> def.id())
                .toList();
    }
}
