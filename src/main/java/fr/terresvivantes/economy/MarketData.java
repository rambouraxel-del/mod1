package fr.terresvivantes.economy;

import fr.terresvivantes.economy.logic.MarketLogic;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Saturation du marché d'une dimension (v0.7) : un état {@link MarketLogic} par identifiant d'item vendu.
 * Stockée par dimension, donc un monde à plusieurs dimensions a des marchés indépendants.
 */
public final class MarketData extends SavedData {

    private static final String NAME = "terresvivantes_market";

    private static final SavedData.Factory<MarketData> FACTORY = new SavedData.Factory<>(
            MarketData::new,
            MarketData::load,
            null);

    private final Map<String, MarketLogic> entries = new HashMap<>();

    public MarketData() {
    }

    private static MarketData load(CompoundTag tag, HolderLookup.Provider registries) {
        MarketData data = new MarketData();
        for (String id : tag.getAllKeys()) {
            CompoundTag entry = tag.getCompound(id);
            data.entries.put(id, new MarketLogic(entry.getDouble("points"), entry.getLong("day")));
        }
        return data;
    }

    /** Marché de la dimension du niveau (créé au premier accès). */
    public static MarketData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Saturation d'un item au jour {@code today}, décroissance comprise. */
    public double pointsAt(String id, long today) {
        MarketLogic state = entries.get(id);
        return state == null ? 0.0 : state.pointsAt(today);
    }

    /** Enregistre une vente d'un item le jour {@code today}. */
    public void sell(String id, long today) {
        entries.put(id, entries.getOrDefault(id, MarketLogic.empty()).sold(today));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<String, MarketLogic> entry : entries.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putDouble("points", entry.getValue().points());
            item.putLong("day", entry.getValue().day());
            tag.put(entry.getKey(), item);
        }
        return tag;
    }
}
