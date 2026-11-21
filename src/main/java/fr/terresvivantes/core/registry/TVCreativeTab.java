package fr.terresvivantes.core.registry;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Onglet créatif unique "Terres Vivantes". Les modules ajoutent leurs items
 * via add(...) pendant leur enregistrement.
 */
public final class TVCreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TerresVivantes.MODID);
    private static final List<Supplier<? extends ItemLike>> ENTRIES = new ArrayList<>();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TerresVivantes.MODID))
                    .icon(() -> new ItemStack(Items.WHEAT))
                    .displayItems((params, output) -> {
                        for (Supplier<? extends ItemLike> entry : ENTRIES) {
                            output.accept(entry.get());
                        }
                    })
                    .build());

    private TVCreativeTab() {
    }

    /** Ajoute un item a l'onglet. A appeler pendant l'enregistrement d'un module. */
    public static void add(Supplier<? extends ItemLike> item) {
        ENTRIES.add(item);
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
