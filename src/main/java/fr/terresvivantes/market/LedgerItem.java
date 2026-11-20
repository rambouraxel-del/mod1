package fr.terresvivantes.market;

import fr.terresvivantes.economy.EconomyApi;
import fr.terresvivantes.market.logic.LedgerLogic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Grand livre du marché (v0.7) : clic droit, le joueur reçoit en chat les prix des produits les plus chers de son inventaire. */
public final class LedgerItem extends Item {

    public LedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            report(serverLevel, serverPlayer);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    /** Écrit les 10 lignes les plus chères de l'inventaire : prix unitaire et part laissée par le marché. */
    private static void report(ServerLevel level, ServerPlayer player) {
        Inventory inventory = player.getInventory();
        long[] prices = new long[inventory.getContainerSize()];
        for (int slot = 0; slot < prices.length; slot++) {
            prices[slot] = EconomyApi.priceOf(level, inventory.getItem(slot));
        }
        List<Integer> top = LedgerLogic.topIndices(prices, LedgerLogic.LINES);
        if (top.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.market.ledger_empty"));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.terresvivantes.market.ledger_title"));
        for (int slot : top) {
            ItemStack stack = inventory.getItem(slot);
            player.sendSystemMessage(Component.translatable("message.terresvivantes.market.ledger_line",
                    stack.getHoverName(), prices[slot], EconomyApi.saturationPercent(level, stack)));
        }
    }
}
