package fr.terresvivantes.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Item de la caisse : garde son contenu (composant) et l'affiche dans l'infobulle. */
public final class CrateItem extends BlockItem {

    public CrateItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        CrateContents contents = stack.get(StorageModule.CRATE_CONTENTS.get());
        if (contents != null && contents.count() > 0) {
            tooltip.add(contentsText(contents));
        }
    }

    /** Texte « x 312 Tomate » pour un contenu. */
    public static Component contentsText(CrateContents contents) {
        return Component.translatable("message.terresvivantes.crate_contents", contents.count(),
                new ItemStack(contents.item()).getHoverName());
    }
}
