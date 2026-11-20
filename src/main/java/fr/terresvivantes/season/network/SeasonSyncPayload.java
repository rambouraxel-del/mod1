package fr.terresvivantes.season.network;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Serveur -> client : saison courante, jour dans la saison et jours par saison. */
public record SeasonSyncPayload(Season season, int dayOfSeason, int daysPerSeason) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SeasonSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "season_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeasonSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> Season.values()[i], Season::ordinal), SeasonSyncPayload::season,
            ByteBufCodecs.VAR_INT, SeasonSyncPayload::dayOfSeason,
            ByteBufCodecs.VAR_INT, SeasonSyncPayload::daysPerSeason,
            SeasonSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
