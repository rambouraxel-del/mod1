package fr.terresvivantes.market;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.network.TVNetwork;
import fr.terresvivantes.market.client.MarketClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;

/** Payloads du tableau d'annonces (v0.7) : ouverture de l'écran (serveur vers client) et acceptation (client vers serveur). */
public final class MarketNetwork {

    private MarketNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(MarketNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(TVNetwork.PROTOCOL_VERSION);
        registrar.playToClient(OpenBoardPayload.TYPE, OpenBoardPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> MarketClient.openBoard(payload.contracts())));
        registrar.playToServer(AcceptContractPayload.TYPE, AcceptContractPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        ContractBoard.accept(player, payload.id());
                    }
                }));
    }

    /** Liste des contrats ouverts, envoyée au joueur qui clique sur le tableau. */
    public record OpenBoardPayload(List<ContractData> contracts) implements CustomPacketPayload {
        public static final Type<OpenBoardPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "open_market_board"));
        public static final StreamCodec<ByteBuf, OpenBoardPayload> STREAM_CODEC =
                ContractData.STREAM_CODEC.apply(ByteBufCodecs.list()).map(OpenBoardPayload::new,
                        OpenBoardPayload::contracts);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Acceptation d'un contrat ouvert par son identifiant. */
    public record AcceptContractPayload(long id) implements CustomPacketPayload {
        public static final Type<AcceptContractPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "accept_market_contract"));
        public static final StreamCodec<ByteBuf, AcceptContractPayload> STREAM_CODEC =
                ByteBufCodecs.VAR_LONG.map(AcceptContractPayload::new, AcceptContractPayload::id);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
