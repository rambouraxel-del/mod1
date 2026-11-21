package fr.terresvivantes.pollination;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.orchard.OrchardApi;
import fr.terresvivantes.pollination.logic.PollinationModel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Module pollination (v0.3) : score de pollinisation et indice de biodiversité, branchés sur les rendements des
 * cultures dépendantes des pollinisateurs et sur la fructification et le rendement des arbres fruitiers.
 * Les autres modules (apiaires, papillons, habitats) s'y enregistrent via {@link PollinationApi} et {@link BiodiversityApi}.
 */
public final class PollinationModule implements TVModule {

    @Override
    public void register(IEventBus modBus) {
        CropGrowthHooks.registerYieldModifier((level, pos, state, def) ->
                def == null ? 1.0f
                        : PollinationModel.cropYieldFactor(def.needsPollination(), PollinationApi.score(level, pos)));
        OrchardApi.registerFruitSetModifier((level, pos, def) ->
                PollinationModel.fruitSetFactor(PollinationApi.score(level, pos)));
        OrchardApi.registerFruitYieldModifier((level, pos, def) ->
                PollinationModel.fruitYieldFactor(PollinationApi.score(level, pos)));
        NeoForge.EVENT_BUS.addListener(PollinationModule::onServerStopping);
    }

    /** Les caches sont liés à un monde : ils sont vidés à l'arrêt du serveur. */
    private static void onServerStopping(ServerStoppingEvent event) {
        PollinationApi.clearCache();
        BiodiversityApi.clearCache();
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("message.terresvivantes.almanac.pollination", "Pollination: %s", "Pollinisation : %s");
        sink.add("message.terresvivantes.almanac.biodiversity", "Biodiversity: %s", "Biodiversité : %s");
    }
}
