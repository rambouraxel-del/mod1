package fr.terresvivantes.crop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.crop.logic.CropDefinitions;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Ajoute, avec une probabilité {@code chance}, une graine aléatoire du mod au loot d'une herbe.
 * Les conditions (table de loot de l'herbe) sont lues dans le JSON de données.
 */
public final class CropSeedLootModifier extends LootModifier {

    public static final MapCodec<CropSeedLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .and(Codec.FLOAT.optionalFieldOf("chance", 0.08f).forGetter(modifier -> modifier.chance))
            .apply(inst, CropSeedLootModifier::new));

    private final float chance;

    public CropSeedLootModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < chance) {
            int index = context.getRandom().nextInt(CropDefinitions.all().size());
            generatedLoot.add(new ItemStack(CropModule.seedItem(CropDefinitions.all().get(index).id())));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
