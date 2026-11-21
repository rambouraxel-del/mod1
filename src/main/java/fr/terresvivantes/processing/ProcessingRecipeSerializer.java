package fr.terresvivantes.processing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.processing.ProcessingRecipe.IngredientEntry;
import fr.terresvivantes.processing.logic.QualityMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
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

    /** Durée maximale d'une recette (ticks) : 2 400 000, soit environ 33 jours de jeu (affinage long). */
    public static final int MAX_TIME = 2_400_000;

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

    /** Ancien format du conteneur : une simple chaîne d'id d'item (rétrocompatible). */
    private static final Codec<Ingredient> ITEM_ID_CONTAINER = BuiltInRegistries.ITEM.byNameCodec()
            .xmap(Ingredient::of, ingredient -> ingredient.getItems()[0].getItem());

    /** Conteneur : un {@link Ingredient} (item ou tag), ou l'ancienne chaîne d'id d'item. */
    private static final Codec<Ingredient> CONTAINER_CODEC = Codec.withAlternative(Ingredient.CODEC, ITEM_ID_CONTAINER);

    private static final MapCodec<ProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("machine").forGetter(ProcessingRecipe::machine),
            ENTRY_CODEC.listOf().fieldOf("ingredients").forGetter(ProcessingRecipe::ingredients),
            CONTAINER_CODEC.optionalFieldOf("container").forGetter(ProcessingRecipe::container),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ProcessingRecipe::result),
            ExtraCodecs.intRange(1, MAX_TIME).fieldOf("time").forGetter(ProcessingRecipe::time),
            QUALITY_CODEC.fieldOf("quality").forGetter(ProcessingRecipe::quality)
    ).apply(instance, ProcessingRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, IngredientEntry> ENTRY_STREAM = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, IngredientEntry::ingredient,
            ByteBufCodecs.VAR_INT, IngredientEntry::count,
            IngredientEntry::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> STREAM = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ProcessingRecipe::machine,
            ByteBufCodecs.collection(ArrayList::new, ENTRY_STREAM), ProcessingRecipe::ingredients,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), ProcessingRecipe::container,
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
