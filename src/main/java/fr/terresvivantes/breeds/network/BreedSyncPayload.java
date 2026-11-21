package fr.terresvivantes.breeds.network;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur -> client : race (identifiant) d'une entité, par identifiant d'entité. */
public record BreedSyncPayload(int entityId, String breedId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BreedSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "breed_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BreedSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BreedSyncPayload::entityId,
            ByteBufCodecs.STRING_UTF8, BreedSyncPayload::breedId,
            BreedSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
