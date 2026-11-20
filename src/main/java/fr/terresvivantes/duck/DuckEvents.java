package fr.terresvivantes.duck;

import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.dairy.logic.MilkSource;
import fr.terresvivantes.duck.logic.ProductionLogic;
import fr.terresvivantes.livestock.LivestockApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Productions liées au génome, branchées sur les interactions joueur :
 * <ul>
 *   <li>tonte d'un mouton : laine supplémentaire selon le multiplicateur (vérification {@code readyForShearing} avant
 *       la tonte vanilla; pas d'événement de tonte NeoForge);</li>
 *   <li>traite d'une vache ou d'une chèvre au seau : le seau de lait reçoit la qualité générique (1 à 3).</li>
 * </ul>
 */
final class DuckEvents {

    private static final int MAX_BASE_WOOL = 3;

    private DuckEvents() {
    }

    static void register(IEventBus bus) {
        bus.addListener(DuckEvents::onEntityInteract);
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity target = event.getTarget();
        ItemStack stack = event.getItemStack();
        boolean server = !event.getLevel().isClientSide();

        if (target instanceof Sheep sheep && stack.is(Items.SHEARS) && sheep.readyForShearing() && server) {
            RandomSource random = sheep.getRandom();
            // Tonte vanilla juste après : le nombre de laine de base est tiré comme celui de la vanilla (1 à 3).
            int baseCount = 1 + random.nextInt(MAX_BASE_WOOL);
            int extra = ProductionLogic.extraWool(LivestockApi.productionMultiplier(sheep), baseCount, random.nextDouble());
            for (int i = 0; i < extra; i++) {
                dropWool(sheep, sheep.getColor());
            }
            return;
        }

        if (target instanceof Animal animal && (animal instanceof Cow || animal instanceof Goat)
                && stack.is(Items.BUCKET) && !animal.isBaby()) {
            // La traite est refaite ici pour poser la qualité sur le seau. Le client ne fait qu'annuler l'action vanilla.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(!server));
            // Convention vanilla : sidedSuccess(isClientSide), donc SUCCESS côté client et CONSUME côté serveur.
            if (!server) {
                return;
            }
            Player player = event.getEntity();
            int quality = ProductionLogic.milkQuality(LivestockApi.productionMultiplier(animal));
            ItemStack milk = new ItemStack(Items.MILK_BUCKET);
            QualityModule.setQuality(milk, quality);
            // Espèce du lait (module dairy) : les recettes de la laiterie distinguent vache et chèvre.
            milk.set(DairyModule.MILK_SOURCE.get(), animal instanceof Goat ? MilkSource.GOAT : MilkSource.COW);
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(stack, player, milk));
        }
    }

    private static void dropWool(Sheep sheep, DyeColor color) {
        Item wool = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_wool"));
        BlockPos pos = sheep.blockPosition();
        ItemEntity drop = new ItemEntity(sheep.level(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                new ItemStack(wool));
        sheep.level().addFreshEntity(drop);
    }
}
