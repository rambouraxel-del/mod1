package fr.terresvivantes.breeds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.breeds.client.BreedClient;
import fr.terresvivantes.breeds.logic.Breed;
import fr.terresvivantes.breeds.network.BreedNetwork;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.vet.VetApi;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Module breeds (v0.9) : races des vaches et des poules. Data Attachment d'entité {@link #BREED} (sérialisé, pas de
 * copyOnDeath), attribution au premier chargement selon le climat, hérédité à la naissance, effets sur la production
 * (via {@code LivestockApi}), synchronisation vers les clients et rendu de la superposition de race (côté client).
 *
 * <p>Façade : {@link BreedsApi}. Règles pures : {@code breeds.logic}.
 */
public final class BreedsModule implements TVModule {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TerresVivantes.MODID);

    /** Codec d'une race : son identifiant. Une valeur inconnue en sauvegarde est refusée. */
    static final Codec<Breed> BREED_CODEC = Codec.STRING.flatXmap(
            id -> Breed.byId(id).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Race inconnue : " + id)),
            breed -> DataResult.success(breed.id()));

    /**
     * Race d'un animal. La valeur par défaut n'est jamais lue : {@link BreedsApi#getBreed} teste d'abord la présence
     * de l'attachment, qui n'est écrit qu'à l'attribution ou à l'hérédité.
     */
    public static final Supplier<AttachmentType<Breed>> BREED = ATTACHMENTS.register("breed",
            () -> AttachmentType.builder(() -> Breed.HOLSTEIN)
                    .serialize(BREED_CODEC)
                    .build());

    @Override
    public void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        BreedNetwork.register(modBus);

        BreedEvents.register(NeoForge.EVENT_BUS);
        BreedCommands.register(NeoForge.EVENT_BUS);
        LivestockApi.registerProductionModifier(BreedsApi::productionModifier);
        LivestockApi.registerNeedsListener(BreedEvents::winterCompensation);
        LivestockApi.registerTwinListener(BreedEvents::onTwin);
        VetApi.registerDiseaseChanceModifier(BreedsApi::diseaseChanceFactor);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            BreedClient.register(modBus);
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("message.terresvivantes.breeds.breed.holstein", "Holstein", "Holstein");
        sink.add("message.terresvivantes.breeds.breed.highland", "Highland", "Highland");
        sink.add("message.terresvivantes.breeds.breed.charolais", "Charolais", "Charolais");
        sink.add("message.terresvivantes.breeds.breed.marans", "Marans", "Marans");
        sink.add("message.terresvivantes.breeds.breed.leghorn", "Leghorn", "Leghorn");
        sink.add("message.terresvivantes.breeds.breed.sussex", "Sussex", "Sussex");
        sink.add("message.terresvivantes.breeds.ledger", "Breed: %1$s", "Race : %1$s");
        sink.add("message.terresvivantes.breeds.command.set", "Breed of %1$s set to %2$s",
                "Race de %1$s : %2$s");
        sink.add("message.terresvivantes.breeds.command.none", "Look at a cow or a chicken.",
                "Regardez une vache ou une poule.");
        sink.add("message.terresvivantes.breeds.command.unknown", "Unknown breed.", "Race inconnue.");
    }
}
