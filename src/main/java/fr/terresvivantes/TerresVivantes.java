package fr.terresvivantes;

import fr.terresvivantes.climate.ClimateModule;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.command.TVCommands;
import fr.terresvivantes.core.debug.DebugModule;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.processing.ProcessingModule;
import fr.terresvivantes.storage.StorageModule;
import fr.terresvivantes.milling.MillingModule;
import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.preserving.PreservingModule;
import fr.terresvivantes.kitchen.KitchenModule;
import fr.terresvivantes.automation.AutomationConfig;
import fr.terresvivantes.automation.AutomationModule;
import fr.terresvivantes.pests.PestsConfig;
import fr.terresvivantes.pests.PestsModule;
import fr.terresvivantes.compost.CompostConfig;
import fr.terresvivantes.core.config.TVConfig;
import fr.terresvivantes.core.network.TVNetwork;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.crop.CropConfig;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.apiculture.ApicultureApi;
import fr.terresvivantes.apiculture.ApicultureConfig;
import fr.terresvivantes.apiculture.ApicultureModule;
import fr.terresvivantes.apiculture.logic.HoneyType;
import fr.terresvivantes.flora.FloraConfig;
import fr.terresvivantes.flora.FloraModule;
import fr.terresvivantes.insects.InsectsConfig;
import fr.terresvivantes.livestock.LivestockConfig;
import fr.terresvivantes.livestock.LivestockModule;
import fr.terresvivantes.husbandry.HusbandryConfig;
import fr.terresvivantes.husbandry.HusbandryModule;
import fr.terresvivantes.vet.VetConfig;
import fr.terresvivantes.vet.VetModule;
import fr.terresvivantes.duck.DuckConfig;
import fr.terresvivantes.duck.DuckModule;
import fr.terresvivantes.insects.InsectsModule;
import fr.terresvivantes.pollination.PollinationModule;
import fr.terresvivantes.datagen.TVDataGen;
import fr.terresvivantes.economy.EconomyModule;
import fr.terresvivantes.guild.GuildModule;
import fr.terresvivantes.market.MarketModule;
import fr.terresvivantes.food.FoodModule;
import fr.terresvivantes.guide.GuideModule;
import fr.terresvivantes.implement.ImplementsModule;
import fr.terresvivantes.health.HealthConfig;
import fr.terresvivantes.health.HealthModule;
import fr.terresvivantes.orchard.OrchardConfig;
import fr.terresvivantes.orchard.OrchardModule;
import fr.terresvivantes.season.SeasonConfig;
import fr.terresvivantes.season.SeasonModule;
import fr.terresvivantes.soil.SoilConfig;
import fr.terresvivantes.soil.SoilModule;
import fr.terresvivantes.tools.ToolsModule;
import fr.terresvivantes.water.WaterConfig;
import fr.terresvivantes.water.WaterModule;
import fr.terresvivantes.vineyard.VineyardConfig;
import fr.terresvivantes.vineyard.VineyardModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import java.util.List;

/**
 * Point d'entree du mod. Seul endroit ou les modules sont listes et enregistres.
 */
@Mod(TerresVivantes.MODID)
public final class TerresVivantes {
    public static final String MODID = "terresvivantes";

    /** Liste fixe des modules. Ajouter un module ici est le seul changement partage autorise. */
    private static final List<TVModule> MODULES = List.of(
            new QualityModule(),
            new ProcessingModule(),
            new SeasonModule(),
            new ClimateModule(),
            new SoilModule(),
            new CropModule(),
            new CompostModule(),
            new FoodModule(),
            new ToolsModule(),
            new OrchardModule(),
            new WaterModule(),
            new HealthModule(),
            new PollinationModule(),
            new FloraModule(),
            new InsectsModule(),
            new ApicultureModule(),
            new LivestockModule(),
            new HusbandryModule(),
            new VetModule(),
            new DuckModule(),
            new MillingModule(),
            new DairyModule(),
            new PreservingModule(),
            new KitchenModule(),
            new ImplementsModule(),
            new AutomationModule(),
            new StorageModule(),
            new PestsModule(),
            new EconomyModule(),
            new MarketModule(),
            new GuildModule(),
            new GuideModule(),
            new VineyardModule(),
            new DebugModule()
    );

    public TerresVivantes(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, TVConfig.COMMON);
        container.registerConfig(ModConfig.Type.COMMON, SeasonConfig.SPEC, SeasonConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, SoilConfig.SPEC, SoilConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, CompostConfig.SPEC, CompostConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, CropConfig.SPEC, CropConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, WaterConfig.SPEC, WaterConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, HealthConfig.SPEC, HealthConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, OrchardConfig.SPEC, OrchardConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, FloraConfig.SPEC, FloraConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, InsectsConfig.SPEC, InsectsConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, ApicultureConfig.SPEC, ApicultureConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, LivestockConfig.SPEC, LivestockConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, HusbandryConfig.SPEC, HusbandryConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, VetConfig.SPEC, VetConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, DuckConfig.SPEC, DuckConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, AutomationConfig.SPEC, AutomationConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, PestsConfig.SPEC, PestsConfig.FILE_NAME);
        container.registerConfig(ModConfig.Type.COMMON, VineyardConfig.SPEC, VineyardConfig.FILE_NAME);
        TVCreativeTab.register(modBus);
        TVNetwork.register(modBus);
        NeoForge.EVENT_BUS.addListener(TVCommands::onRegisterCommands);
        TVDataGen.register(modBus);
        for (TVModule module : MODULES) {
            module.register(modBus);
        }
        // Sources de nectar : les fleurs de la flore donnent leur miel (le reste du tag minecraft:flowers : miel sauvage).
        ApicultureApi.registerNectarSource(state -> state.getBlock() == FloraModule.flower("clover"), HoneyType.CLOVER);
        ApicultureApi.registerNectarSource(state -> state.getBlock() == FloraModule.flower("lavender"), HoneyType.LAVENDER);
    }

    /** Modules enregistres, utilise par datagen pour interroger chaque hook. */
    public static List<TVModule> modules() {
        return MODULES;
    }
}
