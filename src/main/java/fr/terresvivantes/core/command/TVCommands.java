package fr.terresvivantes.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.terresvivantes.core.debug.ShowcaseCommands;
import fr.terresvivantes.season.SeasonCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Racine unique {@code /tv} (operateur niveau 2). Chaque module fournit son sous-arbre :
 * {@code season} (module season), {@code showcase} (démonstration, core.debug).
 */
public final class TVCommands {

    private TVCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tv")
                .requires(source -> source.hasPermission(2))
                .then(SeasonCommands.node())
                .then(ShowcaseCommands.node());
        event.getDispatcher().register(root);
    }
}
