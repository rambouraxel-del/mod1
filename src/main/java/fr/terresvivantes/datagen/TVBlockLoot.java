package fr.terresvivantes.datagen;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.function.BiConsumer;

/** Loot tables du mod, agregees depuis les hooks des modules. */
public final class TVBlockLoot implements LootTableSubProvider {
    private final HolderLookup.Provider registries;

    public TVBlockLoot(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        for (var module : TerresVivantes.modules()) {
            module.registerLootTables(output, registries);
        }
    }
}
