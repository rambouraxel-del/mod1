package fr.terresvivantes.guild;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.terresvivantes.guild.logic.SpecializationLogic;
import fr.terresvivantes.guild.logic.SpecializationLogic.Filiere;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Sous-commande {@code /tv titles} (permission 0). Elle s'ajoute à la racine {@code /tv} de {@code core.command}
 * par fusion de nœuds Brigadier (même littéral : les enfants sont ajoutés, sans toucher au reste).
 */
public final class GuildCommands {

    private GuildCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tv").then(titles()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> titles() {
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
