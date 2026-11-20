package fr.terresvivantes.crop;

import fr.terresvivantes.crop.logic.GrowthDecision;
import fr.terresvivantes.crop.logic.GrowthModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import java.util.List;

/** Écouteurs du module crop sur le bus de jeu NeoForge. */
final class CropEvents {

    /** Graines proposées par le marchand ambulant. */
    private static final List<String> WANDERER_SEEDS = List.of("tomato", "lettuce", "onion", "strawberry");

    private CropEvents() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(CropEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(CropEvents::onCropGrowPre);
        NeoForge.EVENT_BUS.addListener(CropEvents::onCropGrowPost);
        NeoForge.EVENT_BUS.addListener(CropEvents::onWandererTrades);
    }

    /** Clic droit à main vide sur une culture mûre : récolte et replantation, drops au sol. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!CropApi.isMatureCrop(level.getBlockState(pos))) {
            return;
        }
        List<ItemStack> drops = CropApi.harvestAndReplant(level, pos, event.getEntity());
        for (ItemStack stack : drops) {
            Block.popResource(level, pos, stack);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Culture vanilla : croissance annulée selon le facteur (DENY). */
    private static void onCropGrowPre(CropGrowEvent.Pre event) {
        if (!CropConfig.vanillaIntegrationEnabled()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || event.getState().getBlock() instanceof TVCropBlock) {
            return;
        }
        float factor = CropGrowth.vanillaFactor(level, event.getPos());
        if (GrowthModel.decide(factor, level.getRandom().nextDouble(), 0.0) == GrowthDecision.DENY) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }

    /** Culture vanilla qui vient de croître : stade supplémentaire avec chance min(f - 1, 1) (EXTRA_STAGE). */
    private static void onCropGrowPost(CropGrowEvent.Post event) {
        if (!CropConfig.vanillaIntegrationEnabled()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level) || event.getState().getBlock() instanceof TVCropBlock) {
            return;
        }
        BlockState grown = event.getState();
        if (!grown.hasProperty(BlockStateProperties.AGE_7) || grown.getValue(BlockStateProperties.AGE_7) >= 7) {
            return;
        }
        float factor = CropGrowth.vanillaFactor(level, event.getPos());
        if (GrowthModel.decide(factor, 0.0, level.getRandom().nextDouble()) == GrowthDecision.EXTRA_STAGE) {
            level.setBlock(event.getPos(),
                    grown.setValue(BlockStateProperties.AGE_7, grown.getValue(BlockStateProperties.AGE_7) + 1),
                    Block.UPDATE_CLIENTS);
        }
    }

    /** Marchand ambulant : quatre offres de graines. */
    private static void onWandererTrades(WandererTradesEvent event) {
        for (String id : WANDERER_SEEDS) {
            event.getGenericTrades().add(new BasicItemListing(1, new ItemStack(CropModule.seedItem(id), 3), 8, 1));
        }
    }
}
