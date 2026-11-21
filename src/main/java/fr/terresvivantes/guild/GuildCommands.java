package fr.terresvivantes.guild;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.terresvivantes.guild.logic.SpecializationLogic;
import fr.terresvivantes.guild.logic.SpecializationLogic.Filiere;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sous-commande {@code /tv titles} (permission 0). Branchée sur la racine unique {@code /tv} par
 * {@code core.command.TVCommands}.
 */
public final class GuildCommands {

    private GuildCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> titles() {
        return Commands.literal("titles").executes(ctx -> titles(ctx.getSource()));
    }

    private static int titles(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("terresvivantes.guild.command.player_only"));
            return 0;
        }
        for (Filiere filiere : Filiere.values()) {
            Component name = Component.translatable(GuildApi.titleKey(filiere));
            Component line = GuildApi.hasTitle(player, filiere)
                    ? Component.translatable("terresvivantes.guild.titles.obtained", name)
                    : Component.translatable("terresvivantes.guild.titles.progress", name,
                    GuildApi.units(player, filiere), SpecializationLogic.THRESHOLD);
            source.sendSuccess(() -> line, false);
        }
        return Filiere.values().length;
    }
}
