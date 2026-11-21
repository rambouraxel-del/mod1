package fr.terresvivantes.apiculture;

import fr.terresvivantes.apiculture.logic.HoneyType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Cadre de miel : le type de miel est porté par le composant {@link ApicultureModule#HONEY_TYPE}. */
public final class HoneyFrameItem extends Item {

    public HoneyFrameItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        HoneyType type = stack.get(ApicultureModule.HONEY_TYPE.get());
        if (type != null) {
            tooltip.add(Component.translatable("tooltip.terresvivantes.honey_frame",
                    Component.translatable(ApicultureModule.honeyItem(type).getDescriptionId())));
        }
    }
}
