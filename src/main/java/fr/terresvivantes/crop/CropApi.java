package fr.terresvivantes.crop;

import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Façade publique du module crop. */
public final class CropApi {

    private CropApi() {
    }

    /** Vrai si l'état est une culture à son stade final. STUB – implémenté par le module crop (vanilla CropBlock uniquement). */
    public static boolean isMatureCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
    }

    /** Récolte une culture mûre, la remet à l'âge 0 et retourne les drops. STUB – implémenté par le module crop (vanilla CropBlock uniquement). */
    public static List<ItemStack> harvestAndReplant(ServerLevel level, BlockPos pos, Player player) {
        List<ItemStack> drops = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        if (!isMatureCrop(state)) {
            return drops;
        }
        CropBlock crop = (CropBlock) state.getBlock();
        drops.addAll(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, ItemStack.EMPTY));
        level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
        return drops;
    }

    /** Identifiants des cultures optimales pour une saison. STUB – implémenté par le module crop (liste vide). */
    public static List<String> optimalCropIdsFor(Season season) {
        return List.of();
    }
}
