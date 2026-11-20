package fr.terresvivantes.season;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Offset de jours du calendrier, stocke sur l'overworld.
 * dayIndex = floor(dayTime / 24000) + offset.
 */
public final class SeasonSavedData extends SavedData {

    private static final String NAME = "terresvivantes_season";

    private static final SavedData.Factory<SeasonSavedData> FACTORY = new SavedData.Factory<>(
            SeasonSavedData::new,
            SeasonSavedData::load,
            null);

    private long offset;

    public SeasonSavedData() {
    }

    private static SeasonSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SeasonSavedData data = new SeasonSavedData();
        data.offset = tag.getLong("offset");
        return data;
    }

    /** Donnee de saison de l'overworld (creee au premier acces, offset 0). */
    public static SeasonSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long newOffset) {
        if (newOffset != offset) {
            offset = newOffset;
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("offset", offset);
        return tag;
    }
}
