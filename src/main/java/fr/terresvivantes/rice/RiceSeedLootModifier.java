package fr.terresvivantes.rice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Ajoute un plant de riz au loot d'une herbe haute, avec une probabilité {@code chance}, uniquement dans une
 * jungle ou un marais. Les conditions (table de loot de l'herbe) sont lues dans le JSON de données.
 */
public final class RiceSeedLootModifier extends LootModifier {

    public static final MapCodec<RiceSeedLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .and(Codec.FLOAT.optionalFieldOf("chance", 0.06f).forGetter(modifier -> modifier.chance))
            .apply(inst, RiceSeedLootModifier::new));

    private final float chance;

    public RiceSeedLootModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) {
            return generatedLoot;
        }
        ServerLevel level = context.getLevel();
        if (!isWetBiome(level.getBiome(BlockPos.containing(origin)))) {
            return generatedLoot;
        }
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(RiceModule.RICE_SEEDLING.get()));
        }
        return generatedLoot;
    }

    /** Jungles et marais : là où le riz sauvage pousse. */
    static boolean isWetBiome(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_JUNGLE) || biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
