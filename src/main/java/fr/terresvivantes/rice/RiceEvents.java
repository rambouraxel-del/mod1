package fr.terresvivantes.rice;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

/** Écouteurs du module rice sur le bus de jeu NeoForge. */
final class RiceEvents {

    private RiceEvents() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(RiceEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(RiceEvents::onWandererTrades);
    }

    /** Clic droit avec un plant de riz sur une terre : plantation sur l'eau au-dessus, si les conditions sont réunies. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getFace() != Direction.UP) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(RiceModule.RICE_SEEDLING.get())) {
            return;
        }
        Player player = event.getEntity();
        if (!RiceApi.plant(level, event.getPos())) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Marchand ambulant : plants de riz. */
    private static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(new BasicItemListing(1, new ItemStack(RiceModule.RICE_SEEDLING.get(), 3), 8, 1));
    }
}
