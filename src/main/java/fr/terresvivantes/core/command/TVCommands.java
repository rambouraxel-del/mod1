package fr.terresvivantes.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.terresvivantes.core.debug.ShowcaseCommands;
import fr.terresvivantes.economy.EconomyCommands;
import fr.terresvivantes.season.SeasonCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Racine unique {@code /tv}. Chaque module fournit son sous-arbre : {@code season} et {@code showcase}
 * (operateur niveau 2), {@code price} et {@code reputation} (module economy, permission 0).
 */
public final class TVCommands {

    private TVCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tv")
                .then(SeasonCommands.node().requires(source -> source.hasPermission(2)))
                .then(ShowcaseCommands.node().requires(source -> source.hasPermission(2)))
                .then(EconomyCommands.price())
                .then(EconomyCommands.reputation());
        event.getDispatcher().register(root);
    }
}
