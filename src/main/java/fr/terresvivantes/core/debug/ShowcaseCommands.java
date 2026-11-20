package fr.terresvivantes.core.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** {@code /tv showcase [pos]} : plateforme de démonstration, sous-commande de la racine {@code /tv}. */
public final class ShowcaseCommands {

    /** Hauteur au-dessus du joueur quand aucune position n'est donnée. */
    static final int DEFAULT_HEIGHT = 20;
    private static final String POS = "pos";

    private ShowcaseCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("showcase")
                .executes(ctx -> run(ctx.getSource(), null))
                .then(Commands.argument(POS, BlockPosArgument.blockPos())
                        .executes(ctx -> run(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, POS))));
    }

    private static int run(CommandSourceStack source, BlockPos given) {
        ServerLevel level = source.getLevel();
        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
        BlockPos origin;
        if (given != null) {
            origin = given;
        } else if (player != null) {
            origin = player.blockPosition().offset(-ShowcaseBuilder.width() / 2, DEFAULT_HEIGHT, 0);
        } else {
            source.sendFailure(Component.translatable("terresvivantes.command.showcase.need_pos"));
            return 0;
        }
        int placed = ShowcaseBuilder.build(level, origin);
        if (player != null) {
            BlockPos view = ShowcaseBuilder.viewPoint(origin);
            // yaw 0 = regarde vers +Z, donc la plateforme (au sud du point de vue).
            player.teleportTo(level, view.getX() + 0.5, view.getY(), view.getZ() + 0.5,
                    Set.of(), 0.0f, 25.0f);
        }
        source.sendSuccess(() -> Component.translatable("terresvivantes.command.showcase.built",
                origin.toShortString(), placed), true);
        return placed;
    }
}
