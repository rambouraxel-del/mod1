package fr.terresvivantes.season;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.season.logic.SeasonLogic;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Sous-commande {@code /tv season info|set} (la racine {@code /tv} est dans core.command.TVCommands). */
public final class SeasonCommands {

    private SeasonCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("season")
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("set")
                        .then(seasonNode("spring", Season.SPRING))
                        .then(seasonNode("summer", Season.SUMMER))
                        .then(seasonNode("autumn", Season.AUTUMN))
                        .then(seasonNode("winter", Season.WINTER)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> seasonNode(String name, Season season) {
        return Commands.literal(name)
                .executes(ctx -> set(ctx.getSource(), season, 1))
                .then(Commands.argument("day", IntegerArgumentType.integer(1))
                        .executes(ctx -> set(ctx.getSource(), season,
                                IntegerArgumentType.getInteger(ctx, "day"))));
    }

    private static int info(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        ServerLevel overworld = server.overworld();
        long dayIndex = SeasonLogic.dayIndex(overworld.getDayTime(),
                SeasonSavedData.get(overworld).getOffset());
        Season season = SeasonApi.getSeason(overworld);
        int dayOfSeason = SeasonApi.getDayOfSeason(overworld);
        int days = SeasonApi.getDaysPerSeason();
        source.sendSuccess(() -> Component.translatable("terresvivantes.command.season.info",
                Component.translatable(season.langKey()), dayOfSeason, days, dayIndex), false);
        return 1;
    }

    private static int set(CommandSourceStack source, Season season, int day) {
        MinecraftServer server = source.getServer();
        ServerLevel overworld = server.overworld();
        int days = SeasonApi.getDaysPerSeason();
        if (day > days) {
            source.sendFailure(Component.translatable("terresvivantes.command.season.day_out_of_range", days));
            return 0;
        }
        SeasonSavedData data = SeasonSavedData.get(overworld);
        long current = SeasonLogic.dayIndex(overworld.getDayTime(), data.getOffset());
        long target = SeasonLogic.targetDayIndex(current, season, day, days);
        data.setOffset(SeasonLogic.offsetFor(target, overworld.getDayTime()));
        SeasonSync.refresh(server);
        source.sendSuccess(() -> Component.translatable("terresvivantes.command.season.set",
                Component.translatable(season.langKey()), day), true);
        return 1;
    }
}
