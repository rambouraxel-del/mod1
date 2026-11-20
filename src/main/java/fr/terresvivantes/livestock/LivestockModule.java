package fr.terresvivantes.livestock;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.core.registry.TVCreativeTab;
import fr.terresvivantes.datagen.LangSink;
import fr.terresvivantes.livestock.item.LivestockLedgerItem;
import fr.terresvivantes.livestock.logic.Genome;
import fr.terresvivantes.livestock.logic.GenomeLogic;
import fr.terresvivantes.livestock.logic.LivestockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Module livestock (v0.4) : génome et état vital des animaux d'élevage (Data Attachments d'entité), héritage à la
 * naissance, besoins quotidiens, reproduction saisonnière, registre d'élevage (`livestock_ledger`).
 *
 * <p>Façade : {@link LivestockApi}. Règles pures : {@code livestock.logic}.
 */
public final class LivestockModule implements TVModule {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TerresVivantes.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TerresVivantes.MODID);

    /** Génome d'un animal (sérialisé avec l'entité, ne survit pas à la mort : pas de copyOnDeath). */
    public static final Supplier<AttachmentType<Genome>> GENOME = ATTACHMENTS.register("genome",
            () -> AttachmentType.builder(() -> GenomeLogic.randomInitial(Math::random))
                    .serialize(LivestockCodecs.GENOME)
                    .build());

    /** État vital d'un animal : satiété, santé, maladie, dernier repas (sérialisé, pas de copyOnDeath). */
    public static final Supplier<AttachmentType<LivestockState>> STATE = ATTACHMENTS.register("livestock_state",
            () -> AttachmentType.builder(LivestockState::initial)
                    .serialize(LivestockCodecs.STATE)
                    .build());

    /** Registre d'élevage : clic droit sur un animal pour lire ses gènes et son état. */
    public static final DeferredItem<LivestockLedgerItem> LEDGER = ITEMS.register("livestock_ledger",
            () -> new LivestockLedgerItem(new Item.Properties().stacksTo(1)));

    @Override
    public void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);

        TVCreativeTab.add(LEDGER);

        LivestockEvents.register(NeoForge.EVENT_BUS);
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("item.terresvivantes.livestock_ledger", "Livestock Ledger", "Registre d'élevage");
        sink.add("message.terresvivantes.livestock.ledger.title", "Livestock ledger: %1$s", "Registre d'élevage : %1$s");
        sink.add("message.terresvivantes.livestock.ledger.genome",
                "Genes: productivity %1$s, size %2$s, hardiness %3$s, fertility %4$s",
                "Gènes : productivité %1$s, taille %2$s, résistance %3$s, fertilité %4$s");
        sink.add("message.terresvivantes.livestock.ledger.food",
                "Food: %1$s/100 (last meal: day %2$s)", "Nourriture : %1$s/100 (dernier repas : jour %2$s)");
        sink.add("message.terresvivantes.livestock.ledger.health", "Health: %1$s/100, condition: %2$s",
                "Santé : %1$s/100, état : %2$s");
        sink.add("message.terresvivantes.livestock.ledger.ailment.none", "healthy", "en bonne santé");
        sink.add("message.terresvivantes.livestock.ledger.ailment.sick", "sick", "malade");
        sink.add("message.terresvivantes.livestock.ledger.ailment.parasites", "parasites", "parasites");
        sink.add("message.terresvivantes.livestock.ledger.affection.in_love", "Affection: in love",
                "Affection : amoureux");
        sink.add("message.terresvivantes.livestock.ledger.affection.calm", "Affection: calm",
                "Affection : calme");
        sink.add("message.terresvivantes.livestock.ledger.affection.baby", "Affection: too young to breed",
                "Affection : trop jeune pour la reproduction");
        sink.add("message.terresvivantes.livestock.breeding.too_hungry",
                "The animal is too hungry to breed (40 food needed).",
                "L'animal a trop faim pour se reproduire (40 de nourriture requis).");
        sink.add("message.terresvivantes.livestock.breeding.wrong_season",
                "Breeding happens in spring and summer only.",
                "La reproduction n'a lieu qu'au printemps et en été.");
    }

    @Override
    public void registerItemModels(ItemModelProvider provider) {
        // Texture provisoire (almanach) : l'intégrateur basculera sur item/livestock_ledger quand elle sera livrée.
        provider.withExistingParent("livestock_ledger", ResourceLocation.withDefaultNamespace("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "item/almanac"));
    }
}
