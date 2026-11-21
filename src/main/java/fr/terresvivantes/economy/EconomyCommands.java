package fr.terresvivantes.economy;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.terresvivantes.economy.logic.ReputationLogic;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Sous-commandes {@code /tv price} et {@code /tv reputation} (permission 0). Racine dans core.command.TVCommands.
 */
public final class EconomyCommands {

    private EconomyCommands() {
    }

    /** {@code /tv price} : prix actuel, en cuivre, de l'item tenu en main principale. */
    public static LiteralArgumentBuilder<CommandSourceStack> price() {
        return Commands.literal("price").executes(ctx -> price(ctx.getSource()));
    }

    /** {@code /tv reputation} : réputation et palier du joueur. */
    public static LiteralArgumentBuilder<CommandSourceStack> reputation() {
        return Commands.literal("reputation").executes(ctx -> reputation(ctx.getSource()));
    }

    private static int price(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("terresvivantes.command.player_only"));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.translatable("terresvivantes.command.price.hand_empty"));
            return 0;
        }
        int price = EconomyApi.priceOf(player.serverLevel(), held);
        if (price == 0) {
            source.sendFailure(Component.translatable("terresvivantes.command.price.not_sellable"));
            return 0;
        }
        int market = EconomyApi.saturationPercent(player.serverLevel(), held);
        source.sendSuccess(() -> Component.translatable("terresvivantes.command.price.result",
                held.getHoverName(), price, market), false);
        return price;
    }

    private static int reputation(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("terresvivantes.command.player_only"));
            return 0;
        }
        ReputationLogic.Tier tier = EconomyApi.reputationTierOf(player);
        int value = EconomyApi.reputation(player);
        source.sendSuccess(() -> Component.translatable("terresvivantes.command.reputation.info",
                value, Component.translatable(tier.langKey())), false);
        return value;
    }
}
