package fr.terresvivantes.soil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.soil.logic.SoilData;

import java.util.HashMap;
import java.util.Map;

/**
 * Donnees de sol d'un chunk : position de la terre labouree (BlockPos.asLong) vers sa fertilite.
 * Stocke comme Data Attachment de chunk; modifiee sur place, puis chunk.setUnsaved(true).
 */
public final class SoilChunkData {

    private static final Codec<SoilData> SOIL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("n").forGetter(SoilData::n),
            Codec.INT.fieldOf("p").forGetter(SoilData::p),
            Codec.INT.fieldOf("k").forGetter(SoilData::k),
            Codec.INT.fieldOf("organic").forGetter(SoilData::organic)
    ).apply(instance, SoilData::new));

    private static final Codec<Long> KEY_CODEC = Codec.STRING.xmap(Long::parseLong, key -> Long.toString(key));

    /** Codec de persistance : map cle (texte) vers SoilData. */
    public static final Codec<SoilChunkData> CODEC =
            Codec.unboundedMap(KEY_CODEC, SOIL_CODEC).xmap(SoilChunkData::new, data -> data.entries);

    /** Entrees mutables du chunk. Acces reserve au package soil. */
    final Map<Long, SoilData> entries;

    public SoilChunkData() {
        this.entries = new HashMap<>();
    }

    private SoilChunkData(Map<Long, SoilData> loaded) {
        this.entries = new HashMap<>(loaded);
    }
}
