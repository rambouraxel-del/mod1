package fr.terresvivantes.guild;

import fr.terresvivantes.economy.EconomyApi;
import fr.terresvivantes.guild.logic.GuildLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.ArrayList;

/** Écouteurs de la guilde sur le bus de jeu : offres, verrou de réputation, ventes et réputation. */
public final class GuildEvents {

    private GuildEvents() {
    }

    /** Offres du marchand de la guilde, niveau 1 à 5. */
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != GuildModule.GUILD_MERCHANT.get()) {
            return;
        }
        for (int level = GuildLogic.MIN_LEVEL; level <= GuildLogic.MAX_LEVEL; level++) {
            for (VillagerTrades.ItemListing listing : GuildTrades.listingsAt(level)) {
                event.getTrades().computeIfAbsent(level, k -> new ArrayList<>()).add(listing);
            }
        }
    }

    /** Avant l'ouverture des échanges : les offres au-dessus du palier du joueur sont bloquées. */
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getTarget() instanceof Villager villager && event.getEntity() instanceof ServerPlayer player) {
            GuildApi.refreshOffers(player, villager);
        }
    }

    /** Après un échange : +1 réputation, et vente de produit comptée dans sa filière. */
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getAbstractVillager() instanceof Villager villager) || !GuildApi.isGuildMerchant(villager)) {
            return;
        }
        MerchantOffer offer = event.getMerchantOffer();
        EconomyApi.addReputation(player, 1);
        // Seul un achat du marchand (le joueur donne le produit) compte : la pièce payée ne relève d'aucune filière.
        GuildApi.recordSale(player, offer.getCostA());
    }
}
