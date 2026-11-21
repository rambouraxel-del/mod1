package fr.terresvivantes.livestock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.Genome;
import fr.terresvivantes.livestock.logic.LivestockState;

/** Codecs de persistance des données d'élevage (Data Attachments). Les records restent dans {@code logic}, sans codec. */
final class LivestockCodecs {

    static final Codec<Genome> GENOME = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("productivity").forGetter(Genome::productivity),
            Codec.INT.fieldOf("size").forGetter(Genome::size),
            Codec.INT.fieldOf("hardiness").forGetter(Genome::hardiness),
            Codec.INT.fieldOf("fertility").forGetter(Genome::fertility)
    ).apply(instance, Genome::new));

    static final Codec<LivestockState> STATE = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("fed").forGetter(LivestockState::fed),
            Codec.INT.fieldOf("health").forGetter(LivestockState::health),
            Codec.STRING.xmap(Ailment::fromName, Ailment::name).fieldOf("ailment").forGetter(LivestockState::ailment),
            Codec.LONG.fieldOf("last_fed_day").forGetter(LivestockState::lastFedDay)
    ).apply(instance, LivestockState::new));

    private LivestockCodecs() {
    }
}
