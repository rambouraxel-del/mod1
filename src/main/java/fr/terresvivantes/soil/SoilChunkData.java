package fr.terresvivantes.soil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.soil.logic.SoilData;

import java.util.HashMap;
import java.util.Map;

/**
 * Donnees de sol d'un chunk, stockees comme Data Attachment de chunk; modifiees sur place, puis
 * chunk.setUnsaved(true).
 * <ul>
 *   <li>{@code entries} : position de la terre labouree (BlockPos.asLong) vers sa fertilite;</li>
 *   <li>{@code lastCrops} : position de la terre vers sa derniere culture et son compteur de repetition (v0.2).</li>
 * </ul>
 * Le format v0.1 (la seule map positionnelle, a la racine) reste lisible : il est lu comme alternative.
 */
public final class SoilChunkData {

    /** Dernière culture récoltée sur une terre, et nombre de récoltes consécutives de la même culture. */
    public record LastCrop(String cropId, int repeat) {
    }

    private static final Codec<SoilData> SOIL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("n").forGetter(SoilData::n),
            Codec.INT.fieldOf("p").forGetter(SoilData::p),
            Codec.INT.fieldOf("k").forGetter(SoilData::k),
            Codec.INT.fieldOf("organic").forGetter(SoilData::organic)
    ).apply(instance, SoilData::new));

    private static final Codec<LastCrop> LAST_CROP_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("crop").forGetter(LastCrop::cropId),
            Codec.INT.fieldOf("repeat").forGetter(LastCrop::repeat)
    ).apply(instance, LastCrop::new));

    private static final Codec<Long> KEY_CODEC = Codec.STRING.xmap(Long::parseLong, key -> Long.toString(key));

    private static final Codec<Map<Long, SoilData>> ENTRIES_CODEC = Codec.unboundedMap(KEY_CODEC, SOIL_CODEC);

    /** Format v0.2 : {@code soil} (obligatoire) et {@code last_crops} (optionnel, absent avant v0.2). */
    private static final Codec<SoilChunkData> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC.fieldOf("soil").forGetter(data -> data.entries),
            Codec.unboundedMap(KEY_CODEC, LAST_CROP_CODEC).optionalFieldOf("last_crops", Map.of())
                    .forGetter(data -> data.lastCrops)
    ).apply(instance, SoilChunkData::new));

    /** Codec de persistance. La lecture accepte le format v0.1 (map positionnelle seule) comme repli. */
    public static final Codec<SoilChunkData> CODEC = Codec.withAlternative(CURRENT_CODEC, ENTRIES_CODEC,
            entries -> new SoilChunkData(entries, Map.of()));

    /** Entrees mutables du chunk. Acces reserve au package soil. */
    final Map<Long, SoilData> entries;

    /** Dernieres cultures mutables du chunk. Acces reserve au package soil. */
    final Map<Long, LastCrop> lastCrops;

    public SoilChunkData() {
        this(Map.of(), Map.of());
    }

    private SoilChunkData(Map<Long, SoilData> entries, Map<Long, LastCrop> lastCrops) {
        this.entries = new HashMap<>(entries);
        this.lastCrops = new HashMap<>(lastCrops);
    }
}
