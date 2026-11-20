package fr.terresvivantes;

import fr.terresvivantes.climate.ClimateModule;
import fr.terresvivantes.compost.CompostModule;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.config.TVConfig;
import fr.terresvivantes.core.network.TVNetwork;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.crop.CropModule;
import fr.terresvivantes.datagen.TVDataGen;
import fr.terresvivantes.food.FoodModule;
import fr.terresvivantes.season.SeasonModule;
import fr.terresvivantes.soil.SoilModule;
import fr.terresvivantes.tools.ToolsModule;
import net.neoforged.bus.api.IEventBus;
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
            new SeasonModule(),
            new ClimateModule(),
            new SoilModule(),
            new CropModule(),
            new CompostModule(),
            new FoodModule(),
            new ToolsModule()
    );

    public TerresVivantes(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, TVConfig.COMMON);
        TVCreativeTab.register(modBus);
        TVNetwork.register(modBus);
        TVDataGen.register(modBus);
        for (TVModule module : MODULES) {
            module.register(modBus);
        }
    }

    /** Modules enregistres, utilise par datagen pour interroger chaque hook. */
    public static List<TVModule> modules() {
        return MODULES;
    }
}
