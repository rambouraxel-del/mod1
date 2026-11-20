package fr.terresvivantes.health;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.health.logic.CropHealth;
import fr.terresvivantes.health.logic.Disease;

import java.util.HashMap;
import java.util.Map;

/**
 * Santé des cultures d'un chunk, stockée en Data Attachment de chunk. Clé : {@code BlockPos.asLong()} de la culture.
 * Une culture saine sans stress n'a pas d'entrée. Les entrées des blocs qui ne sont plus une culture sont retirées
 * ({@link HealthApi}).
 */
public final class HealthChunkData {

    private static final Codec<Long> KEY_CODEC = Codec.STRING.xmap(Long::parseLong, key -> Long.toString(key));

    private static final Codec<Disease> DISEASE_CODEC = Codec.STRING.xmap(Disease::valueOf, Disease::name);

    private static final Codec<CropHealth> HEALTH_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("stress").forGetter(CropHealth::stress),
            DISEASE_CODEC.fieldOf("disease").forGetter(CropHealth::disease),
            Codec.INT.fieldOf("disease_ticks").forGetter(CropHealth::diseaseTicks),
            Codec.LONG.fieldOf("immune_until").forGetter(CropHealth::immuneUntilDay)
    ).apply(instance, CropHealth::new));

    /** Persistance : map position -> santé. */
    public static final Codec<HealthChunkData> CODEC =
            Codec.unboundedMap(KEY_CODEC, HEALTH_CODEC).xmap(HealthChunkData::new, data -> data.entries);

    /** Entrées mutables du chunk. Acces reserve au package health. */
    final Map<Long, CropHealth> entries;

    public HealthChunkData() {
        this(Map.of());
    }

    private HealthChunkData(Map<Long, CropHealth> entries) {
        this.entries = new HashMap<>(entries);
    }
}
