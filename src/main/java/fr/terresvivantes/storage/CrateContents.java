package fr.terresvivantes.storage;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

/**
 * Contenu d'une caisse, porté par l'item de caisse quand elle est cassée (composant {@code terresvivantes:crate_contents}).
 */
public record CrateContents(Item item, int count) {

    public static final Codec<CrateContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CrateContents::item),
            Codec.INT.fieldOf("count").forGetter(CrateContents::count)
    ).apply(instance, CrateContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrateContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), CrateContents::item,
            ByteBufCodecs.VAR_INT, CrateContents::count,
            CrateContents::new);
}
