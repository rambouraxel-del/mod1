package fr.terresvivantes.season.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du module season et de son effet sur le climat. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class SeasonGameTests {

    private SeasonGameTests() {
    }

    /**
     * /tv season set change la saison lue par SeasonApi, et l'hiver est plus froid que l'ete au meme endroit.
     * Utilise la structure vide {@code modloads} (meme structure que le test de fumee du core).
     */
    @GameTest(template = "modloads")
    public static void seasonSetChangesSeasonAndTemperature(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set summer 1");
        helper.assertTrue(SeasonApi.getSeason(level) == Season.SUMMER,
                "apres /tv season set summer, la saison doit etre ete");
        float summer = ClimateApi.temperatureAt(level, pos);

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set winter 1");
        helper.assertTrue(SeasonApi.getSeason(level) == Season.WINTER,
                "apres /tv season set winter, la saison doit etre hiver");
        float winter = ClimateApi.temperatureAt(level, pos);

        helper.assertTrue(winter < summer,
                "hiver (" + winter + " °C) doit etre plus froid que ete (" + summer + " °C) au meme endroit");
        helper.succeed();
    }
}
