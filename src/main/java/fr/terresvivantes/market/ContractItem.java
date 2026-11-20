package fr.terresvivantes.market;

import fr.terresvivantes.core.quality.logic.QualityLogic;
import fr.terresvivantes.market.client.MarketClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Contrat accepté (v0.7) : une feuille qui décrit la livraison attendue. Les données sont dans le composant. */
public final class ContractItem extends Item {

    public ContractItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        ContractData data = stack.get(MarketModule.CONTRACT_DATA.get());
        if (data == null) {
            return;
        }
        Item product = itemOf(data.product());
        tooltip.add(Component.translatable("tooltip.terresvivantes.contract.deliver",
                Component.translatable(product.getDescriptionId()), data.quantity()));
        tooltip.add(Component.translatable("tooltip.terresvivantes.contract.quality",
                QualityLogic.stars(data.minQuality())));
        tooltip.add(Component.translatable("tooltip.terresvivantes.contract.deadline", data.deadlineDay()));
        tooltip.add(Component.translatable("tooltip.terresvivantes.contract.reward", data.reward()));
        if (MarketClient.isExpired(data.deadlineDay())) {
            tooltip.add(Component.translatable("tooltip.terresvivantes.contract.expired"));
        }
    }

    /** Item d'un identifiant complet, ou l'air si l'identifiant est inconnu. */
    public static Item itemOf(String product) {
        ResourceLocation id = ResourceLocation.tryParse(product);
        return id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
    }
}
