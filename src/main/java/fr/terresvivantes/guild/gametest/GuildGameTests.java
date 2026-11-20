package fr.terresvivantes.guild.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.economy.logic.ReputationLogic.Tier;
import fr.terresvivantes.guild.GuildApi;
import fr.terresvivantes.guild.GuildModule;
import fr.terresvivantes.guild.GuildTrades;
import fr.terresvivantes.guild.logic.SpecializationLogic;
import fr.terresvivantes.guild.logic.SpecializationLogic.Filiere;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu de la guilde : bureau (POI) et profession, verrou de réputation, titre de spécialisation. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class GuildGameTests {

    private static final String TEMPLATE = "orchard_arena";
    private static final String BATCH = "guild";

    private GuildGameTests() {
    }

    private static ItemStack stack(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, path)).orElseThrow());
    }

    /** Le bureau est un POI du profession, et un villageois qui exerce la profession a des offres. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void guildDeskIsTheGuildMerchantJobSite(GameTestHelper helper) {
        BlockState desk = GuildModule.GUILD_DESK.get().defaultBlockState();
        helper.assertTrue(PoiTypes.forState(desk).map(holder -> holder.is(GuildModule.GUILD_DESK_POI_KEY)).orElse(false),
                "le bureau de la guilde doit être un POI guild_desk");

        Villager villager = helper.spawn(EntityType.VILLAGER, 2, 2, 2);
        villager.setVillagerData(villager.getVillagerData().setProfession(GuildModule.GUILD_MERCHANT.get()));
        helper.assertTrue(GuildApi.isGuildMerchant(villager), "le villageois doit exercer guild_merchant");
        helper.assertTrue(!villager.getOffers().isEmpty(), "le marchand de la guilde doit proposer des offres");
        helper.succeed();
    }

    /** Une offre de niveau 3 est bloquée pour un Habitué (réputation 50) et ouverte pour un Fournisseur (200). */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void levelThreeOfferIsBlockedUnderSupplierTier(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityType.VILLAGER, 2, 2, 2);
        MerchantOffer locked = GuildTrades.listingsAt(3).get(0).getOffer(villager, helper.getLevel().getRandom());
        helper.assertTrue(GuildApi.applyTierLock(locked, Tier.REGULAR), "offre de niveau 3 attendue bloquée sous Habitué");
        helper.assertTrue(locked.isOutOfStock(), "l'offre bloquée doit être en rupture de stock");

        MerchantOffer open = GuildTrades.listingsAt(3).get(0).getOffer(villager, helper.getLevel().getRandom());
        helper.assertTrue(!GuildApi.applyTierLock(open, Tier.SUPPLIER), "offre de niveau 3 ouverte pour un Fournisseur");
        helper.assertTrue(!open.isOutOfStock(), "l'offre ouverte ne doit pas être en rupture");
        helper.succeed();
    }

    /** 256 tomates vendues au marchand : titre Maraîcher, un sceau de la guilde, et bonus de qualité pour la filière. */
    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void tomatoSalesGrantMarketGardenerTitle(GameTestHelper helper) {
        // Joueur factice sans connexion : un ServerPlayer en jeu déclenche le payload season_sync, refusé ici.
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack tomato = stack("tomato");
        for (int i = 0; i < SpecializationLogic.THRESHOLD - 1; i++) {
            GuildApi.recordSale(player, tomato);
        }
        helper.assertTrue(!GuildApi.hasTitle(player, Filiere.MARAICHER), "255 ventes ne donnent pas encore le titre");
        GuildApi.recordSale(player, tomato);
        helper.assertTrue(GuildApi.hasTitle(player, Filiere.MARAICHER), "256 tomates doivent donner le titre Maraîcher");
        helper.assertTrue(player.getInventory().countItem(GuildModule.GUILD_SEAL.get()) == 1,
                "le titre doit remettre un sceau de la guilde");
        helper.assertTrue(GuildApi.qualityBonusChance(player, "maraicher") == SpecializationLogic.QUALITY_BONUS_CHANCE,
                "le titre Maraîcher donne le bonus de qualité de sa filière");
        helper.assertTrue(GuildApi.qualityBonusChance(player, "eleveur") == 0f,
                "sans titre d'Éleveur, pas de bonus");
        helper.succeed();
    }
}
