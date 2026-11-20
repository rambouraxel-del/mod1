package fr.terresvivantes.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.market.logic.Contract;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Données d'un contrat portées par l'item {@code contract} (composant {@link MarketModule#CONTRACT_DATA}) et
 * envoyées au client pour le tableau. Sans état : l'état réel du contrat reste dans {@link ContractBoardData}.
 */
public record ContractData(long id, String product, int quantity, int minQuality, long deadlineDay, int reward,
                           int reputation) {

    public static final Codec<ContractData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("id").forGetter(ContractData::id),
            Codec.STRING.fieldOf("product").forGetter(ContractData::product),
            Codec.INT.fieldOf("quantity").forGetter(ContractData::quantity),
            Codec.INT.fieldOf("min_quality").forGetter(ContractData::minQuality),
            Codec.LONG.fieldOf("deadline_day").forGetter(ContractData::deadlineDay),
            Codec.INT.fieldOf("reward").forGetter(ContractData::reward),
            Codec.INT.fieldOf("reputation").forGetter(ContractData::reputation)
    ).apply(i, ContractData::new));

    /** Même forme que le codec persistant, sérialisée pour le réseau (le contrat est petit). */
    public static final StreamCodec<ByteBuf, ContractData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** Données d'un contrat du tableau. */
    public static ContractData of(Contract contract) {
        return new ContractData(contract.id(), contract.product(), contract.quantity(), contract.minQuality(),
                contract.deadlineDay(), contract.reward(), contract.reputation());
    }
}
