package fr.terresvivantes.core.quality;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.quality.logic.QualityLogic;
import fr.terresvivantes.datagen.LangSink;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Qualité générique (v0.5) : composant {@code terresvivantes:quality} (entier 1 à 3) posé sur tout item qui le porte.
 * L'infobulle affiche les étoiles sur n'importe quel item porteur, sans code propre au module. Les modules posent
 * la qualité; la logique (héritage, vieillissement) vit dans {@link QualityLogic}.
 */
public final class QualityModule implements TVModule {

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TerresVivantes.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> QUALITY =
            COMPONENTS.registerComponentType("quality", builder -> builder
                    .persistent(ExtraCodecs.intRange(QualityLogic.MIN, QualityLogic.MAX))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    @Override
    public void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(QualityModule::onTooltip);
    }

    /** Qualité d'un stack, ou {@link QualityLogic#ABSENT} si le composant est absent. */
    public static int qualityOf(ItemStack stack) {
        Integer quality = stack.get(QUALITY.get());
        return quality == null ? QualityLogic.ABSENT : quality;
    }

    /** Pose la qualité (bornée entre 1 et 3) sur un stack. */
    public static void setQuality(ItemStack stack, int quality) {
        stack.set(QUALITY.get(), QualityLogic.clamp(quality));
    }

    private static void onTooltip(ItemTooltipEvent event) {
        int quality = qualityOf(event.getItemStack());
        if (quality != QualityLogic.ABSENT) {
            event.getToolTip().add(Component.translatable("tooltip.terresvivantes.quality",
                    QualityLogic.stars(quality)));
        }
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("tooltip.terresvivantes.quality", "Quality: %s", "Qualité: %s");
    }
}
