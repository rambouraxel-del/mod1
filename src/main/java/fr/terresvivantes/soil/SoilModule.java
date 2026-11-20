package fr.terresvivantes.soil;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.datagen.LangSink;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Module Soil : fertilite des terres labourees, stockee en Data Attachment de chunk. */
public final class SoilModule implements TVModule {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TerresVivantes.MODID);

    /** Donnees de sol par chunk, serialisees avec le chunk. */
    public static final Supplier<AttachmentType<SoilChunkData>> CHUNK_DATA = ATTACHMENTS.register("soil",
            () -> AttachmentType.builder(() -> new SoilChunkData()).serialize(SoilChunkData.CODEC).build());

    @Override
    public void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(SoilModule::onBlockBreak);
        CropGrowthHooks.registerModifier(new SoilRotation());
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("message.terresvivantes.soil_probe.last_crop",
                "Last crop: %s (repeats: %s)", "Dernière culture : %s (répétitions : %s)");
        sink.add("message.terresvivantes.soil_probe.rotation_monoculture",
                "Monoculture: growth penalty if the same crop is planted again",
                "Monoculture : malus de croissance si la même culture est replantée");
        sink.add("message.terresvivantes.soil_probe.rotation_legume",
                "Previous legume: +15% growth for a non-legume crop",
                "Précédent légumineuse : +15 % de croissance pour une culture non légumineuse");
        sink.add("message.terresvivantes.soil_probe.last_crop_none",
                "Last crop: none", "Dernière culture : aucune");
        sink.add("message.terresvivantes.soil_probe.rotation_none",
                "No rotation effect", "Pas d'effet de rotation");
    }

    /** Efface la fertilite quand une terre labouree est cassee. Le reste est gere par la verification paresseuse. */
    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level) {
            SoilApi.clear(level, event.getPos());
        }
    }
}
