package fr.terresvivantes.season;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.season.client.SeasonHud;
import fr.terresvivantes.season.network.SeasonNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Module Season : cycle des saisons, commandes, synchronisation et HUD (cote client, classe a part). */
public final class SeasonModule implements TVModule {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TerresVivantes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);

    @Override
    public void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(SeasonNetwork::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(SeasonSync::onLevelTick);
        NeoForge.EVENT_BUS.addListener(SeasonSync::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(SeasonSync::onServerStopped);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SeasonHud.register(modBus);
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("terresvivantes.season.spring", "Spring", "Printemps");
        sink.add("terresvivantes.season.summer", "Summer", "Été");
        sink.add("terresvivantes.season.autumn", "Autumn", "Automne");
        sink.add("terresvivantes.season.winter", "Winter", "Hiver");
        sink.add("terresvivantes.season.changed", "New season: %1$s.", "Nouvelle saison : %1$s.");
        sink.add("terresvivantes.hud.season", "%1$s · Day %2$s/%3$s", "%1$s · Jour %2$s/%3$s");
        sink.add("terresvivantes.command.season.info",
                "Season: %1$s, day %2$s/%3$s (day index %4$s)",
                "Saison : %1$s, jour %2$s/%3$s (index de jour %4$s)");
        sink.add("terresvivantes.command.season.set", "Season set to %1$s, day %2$s.",
                "Saison réglée sur %1$s, jour %2$s.");
        sink.add("terresvivantes.command.season.day_out_of_range",
                "Day must be between 1 and %1$s.", "Le jour doit être compris entre 1 et %1$s.");
    }
}
