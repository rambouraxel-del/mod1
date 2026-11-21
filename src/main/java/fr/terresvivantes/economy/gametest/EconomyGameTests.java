package fr.terresvivantes.economy.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.economy.EconomyApi;
import fr.terresvivantes.economy.EconomyModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du module economy : prix saisonnier, saturation du marché, réputation persistante. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class EconomyGameTests {

    private EconomyGameTests() {
    }

    private static ItemStack stack(String path) {
        Item item = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, path))
                .orElseThrow();
        return new ItemStack(item);
    }

    /** Une tomate (saison optimale : été) coûte moins cher en été qu'en hiver. */
    @GameTest(template = "modloads")
    public static void tomatoOutOfSeasonCostsMoreThanInSeason(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ItemStack tomato = stack("tomato");

        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set summer 1");
        int inSeason = EconomyApi.priceOf(level, tomato);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tv season set winter 1");
        int outOfSeason = EconomyApi.priceOf(level, tomato);

        helper.assertTrue(inSeason > 0, "la tomate doit être vendable");
        helper.assertTrue(outOfSeason > inSeason,
                "hors saison (" + outOfSeason + ") doit dépasser la pleine saison (" + inSeason + ")");
        helper.succeed();
    }

    /**
     * Cinquante ventes d'un même item amènent le marché au plancher (40 % de la base) et ne font jamais monter le prix.
     * L'assertion ne suppose pas un marché neuf : le monde de test est sauvegardé entre deux lancements, et 50 points
     * ajoutés le même jour suffisent à atteindre le plancher quel que soit l'état de départ.
     */
    @GameTest(template = "modloads")
    public static void fiftySalesReachTheMarketFloor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack corn = stack("corn");
        int before = EconomyApi.priceOf(level, corn);
        for (int i = 0; i < 50; i++) {
            EconomyApi.recordSale(level, corn);
        }
        int after = EconomyApi.priceOf(level, corn);
        helper.assertTrue(EconomyApi.saturationPercent(level, corn) == 40,
                "après 50 ventes le marché doit être au plancher (40 %), reçu " + EconomyApi.saturationPercent(level, corn));
        helper.assertTrue(after <= before,
                "après 50 ventes le prix (" + after + ") ne doit pas dépasser le prix initial (" + before + ")");
        helper.succeed();
    }

    /**
     * La réputation survit à une sauvegarde NBT du joueur (attachment persistant). Joueur factice sans connexion :
     * une connexion réelle déclenche le payload season_sync, que le client de test ne sait pas recevoir.
     */
    @GameTest(template = "modloads")
    public static void reputationPersists(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setData(EconomyModule.REPUTATION.get(), 120);
        helper.assertTrue(player.getData(EconomyModule.REPUTATION.get()) == 120, "réputation attendue 120 avant sauvegarde");

        CompoundTag tag = new CompoundTag();
        player.saveWithoutId(tag);
        Player reloaded = helper.makeMockPlayer(GameType.SURVIVAL);
        reloaded.load(tag);
        helper.assertTrue(reloaded.getData(EconomyModule.REPUTATION.get()) == 120,
                "réputation après rechargement : " + reloaded.getData(EconomyModule.REPUTATION.get()) + " au lieu de 120");
        helper.succeed();
    }
}
