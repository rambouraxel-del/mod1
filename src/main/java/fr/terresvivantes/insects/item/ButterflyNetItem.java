package fr.terresvivantes.insects.item;

import fr.terresvivantes.insects.InsectsModule;
import fr.terresvivantes.insects.entity.Butterfly;
import fr.terresvivantes.insects.entity.Ladybug;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Filet à papillons (v0.3). Clic droit sur un papillon ou une coccinelle : l'insecte est capturé dans le bocal
 * correspondant, et une bouteille en verre de l'inventaire est consommée. Sans bouteille, rien ne se passe.
 */
public final class ButterflyNetItem extends Item {

    public ButterflyNetItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        Item jar;
        int variant = 0;
        if (target instanceof Butterfly butterfly) {
            jar = InsectsModule.BUTTERFLY_JAR.get();
            variant = butterfly.getVariant();
        } else if (target instanceof Ladybug) {
            jar = InsectsModule.LADYBUG_JAR.get();
        } else {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int bottleSlot = findBottle(player);
        if (bottleSlot < 0) {
            player.displayClientMessage(Component.translatable("message.terresvivantes.insects.no_bottle"), true);
            return InteractionResult.FAIL;
        }
        player.getInventory().getItem(bottleSlot).shrink(1);
        ItemStack jarStack = new ItemStack(jar);
        jarStack.set(InsectsModule.INSECT_VARIANT.get(), variant);
        target.discard();
        if (!player.getInventory().add(jarStack)) {
            player.drop(jarStack, false);
        }
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResult.SUCCESS;
    }

    /** Emplacement d'une bouteille en verre dans l'inventaire, ou -1. */
    private static int findBottle(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.GLASS_BOTTLE)) {
                return slot;
            }
        }
        return -1;
    }
}
