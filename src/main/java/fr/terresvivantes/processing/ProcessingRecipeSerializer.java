package fr.terresvivantes.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.processing.ProcessingRecipe.IngredientEntry;
import fr.terresvivantes.processing.logic.QualityMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;

/**
 * Sérialiseur de {@link ProcessingRecipe}. Format JSON :
 * <pre>
 * {
 *   "type": "terresvivantes:processing",
 *   "machine": "millstone",
 *   "ingredients": [ { "ingredient": { "item": "minecraft:wheat" }, "count": 1 } ],
 *   "container": "minecraft:glass_bottle",        (optionnel)
 *   "result": { "id": "terresvivantes:wheat_flour", "count": 1 },
 *   "time": 100,
 *   "quality": "inherit_min"                       (none | inherit_min | inherit_avg | age)
 * }
 * </pre>
 */
public final class ProcessingRecipeSerializer implements RecipeSerializer<ProcessingRecipe> {

    private static final Codec<QualityMode> QUALITY_CODEC = Codec.STRING.comapFlatMap(id -> {
        try {
            return DataResult.success(QualityMode.fromId(id));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }, QualityMode::id);

    private static final Codec<IngredientEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(IngredientEntry::ingredient),
            ExtraCodecs.intRange(1, 64).optionalFieldOf("count", 1).forGetter(IngredientEntry::count)
    ).apply(instance, IngredientEntry::new));

    private static final MapCodec<ProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("machine").forGetter(ProcessingRecipe::machine),
            ENTRY_CODEC.listOf().fieldOf("ingredients").forGetter(ProcessingRecipe::ingredients),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("container").forGetter(ProcessingRecipe::container),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ProcessingRecipe::result),
            ExtraCodecs.intRange(1, 72000).fieldOf("time").forGetter(ProcessingRecipe::time),
            QUALITY_CODEC.fieldOf("quality").forGetter(ProcessingRecipe::quality)
    ).apply(instance, ProcessingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, IngredientEntry> ENTRY_STREAM = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, IngredientEntry::ingredient,
            ByteBufCodecs.VAR_INT, IngredientEntry::count,
            IngredientEntry::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ProcessingRecipe::machine,
            ByteBufCodecs.collection(ArrayList::new, ENTRY_STREAM), ProcessingRecipe::ingredients,
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM)), ProcessingRecipe::container,
            ItemStack.STREAM_CODEC, ProcessingRecipe::result,
            ByteBufCodecs.VAR_INT, ProcessingRecipe::time,
            ByteBufCodecs.STRING_UTF8.map(QualityMode::fromId, QualityMode::id), ProcessingRecipe::quality,
            ProcessingRecipe::new);

    @Override
    public MapCodec<ProcessingRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec() {
        return STREAM;
    }
}
