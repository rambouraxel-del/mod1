package fr.terresvivantes.breeds.client;

import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Côté client : superposition de race sur les vaches et les poules. Chargé seulement sur le client. */
public final class BreedClient {

    private BreedClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BreedClient::onAddLayers);
        NeoForge.EVENT_BUS.addListener(BreedClient::onLoggingOut);
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer<Cow, CowModel<Cow>> cow = event.getRenderer(EntityType.COW);
        if (cow != null) {
            cow.addLayer(new BreedLayer<>(cow));
        }
        LivingEntityRenderer<Chicken, ChickenModel<Chicken>> chicken = event.getRenderer(EntityType.CHICKEN);
        if (chicken != null) {
            chicken.addLayer(new BreedLayer<>(chicken));
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BreedClientCache.clear();
    }
}
