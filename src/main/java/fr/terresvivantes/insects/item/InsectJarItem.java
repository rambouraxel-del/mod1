package fr.terresvivantes.insects.item;

import fr.terresvivantes.insects.InsectsModule;
import fr.terresvivantes.insects.entity.Butterfly;
import fr.terresvivantes.insects.logic.ButterflyLogic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.function.Supplier;

/**
 * Bocal à insecte (v0.3) : contient un papillon ou une coccinelle, avec sa variante (composant de données).
 * Clic droit sur un bloc : l'insecte est relâché sur la face cliquée, et le bocal redevient une bouteille.
 */
public final class InsectJarItem extends Item {

    private final Supplier<? extends EntityType<? extends Mob>> insect;

    public InsectJarItem(Supplier<? extends EntityType<? extends Mob>> insect, Properties properties) {
        super(properties);
        this.insect = insect;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        Entity entity = this.insect.get().spawn(server, spawnPos, MobSpawnType.MOB_SUMMONED);
        if (entity instanceof Butterfly butterfly) {
            butterfly.setVariant(variantOf(stack));
        }
        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        if (this.insect.get() == InsectsModule.BUTTERFLY.get()) {
            tooltip.add(Component.translatable("message.terresvivantes.insects.variant." + variantKey(stack)));
        }
    }

    /** Variante d'un bocal à papillon (0 si le bocal n'en porte pas). */
    public static int variantOf(ItemStack stack) {
        return ButterflyLogic.clampVariant(stack.getOrDefault(InsectsModule.INSECT_VARIANT.get(), 0));
    }

    private static String variantKey(ItemStack stack) {
        return switch (variantOf(stack)) {
            case 1 -> "cabbage_white";
            case 2 -> "blue_morpho";
            case 3 -> "brimstone";
            default -> "monarch";
        };
    }
}
