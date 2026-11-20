package fr.terresvivantes.livestock;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.Genome;
import fr.terresvivantes.livestock.logic.GenomeLogic;
import fr.terresvivantes.livestock.logic.LivestockLogic;
import fr.terresvivantes.livestock.logic.LivestockState;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Façade du module livestock. Les autres modules passent par cette classe, jamais par les attachments directement.
 *
 * <p>Les animaux concernés ont le tag d'entités {@link #LIVESTOCK_TAG}. Les hooks ci-dessous sont enregistrés à
 * l'initialisation, comme {@code CropGrowthHooks}.
 */
public final class LivestockApi {

    /** Tag d'entités des animaux d'élevage (data/terresvivantes/tags/entity_type/livestock.json). */
    public static final TagKey<EntityType<?>> LIVESTOCK_TAG = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "livestock"));

    /** Multiplicateur de production propre à un module (ex. : productions, lait de chèvre). 1.0 neutre. */
    @FunctionalInterface
    public interface ProductionModifier {
        float modify(Animal animal);
    }

    /**
     * Source de nourriture (ex. : mangeoire du module husbandry). Appelée à chaque cycle de besoins de chaque animal;
     * renvoie les points de satiété ajoutés (0 si rien).
     */
    @FunctionalInterface
    public interface FeedSource {
        int feed(ServerLevel level, Animal animal);
    }

    private static final List<ProductionModifier> PRODUCTION_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<FeedSource> FEED_SOURCES = new CopyOnWriteArrayList<>();

    private LivestockApi() {
    }

    /** Vrai si l'entité a le tag des animaux d'élevage. */
    public static boolean isLivestock(Entity entity) {
        return entity.getType().is(LIVESTOCK_TAG);
    }

    /** Vrai si l'espèce broute (pâturage sur herbe hors hiver) : vache, vache à champignons, mouton, chèvre, lapin. */
    public static boolean isGrazer(Entity entity) {
        EntityType<?> type = entity.getType();
        return type == EntityType.COW || type == EntityType.MOOSHROOM || type == EntityType.SHEEP
                || type == EntityType.GOAT || type == EntityType.RABBIT;
    }

    /** Génome de l'animal. Un animal sans génome reçoit un génome initial aléatoire. */
    public static Genome getGenome(Animal animal) {
        return animal.getData(LivestockModule.GENOME.get());
    }

    /** État vital de l'animal (satiété, santé, maladie, dernier repas). */
    public static LivestockState getState(Animal animal) {
        return animal.getData(LivestockModule.STATE.get());
    }

    /** Écrit l'état vital. Réservé aux modules livestock, vet et husbandry. */
    public static void setState(Animal animal, LivestockState state) {
        animal.setData(LivestockModule.STATE.get(), state);
    }

    /** Ajoute {@code amount} points de satiété (borné à 100) et note le jour du repas. */
    public static void feed(Animal animal, int amount) {
        LivestockState state = getState(animal);
        long day = animal.level() instanceof ServerLevel level ? SeasonApi.dayIndex(level) : state.lastFedDay();
        setState(animal, state.withFed(state.fed() + amount).withLastFedDay(amount > 0 ? day : state.lastFedDay()));
    }

    /** Pose ou retire une maladie. */
    public static void setAilment(Animal animal, Ailment ailment) {
        setState(animal, getState(animal).withAilment(ailment));
    }

    /**
     * Multiplicateur de production de l'animal : formule de {@link LivestockLogic#productionMultiplier} (génétique,
     * satiété, santé), multiplié par les modificateurs enregistrés.
     */
    public static float productionMultiplier(Animal animal) {
        Genome genome = getGenome(animal);
        LivestockState state = getState(animal);
        float value = LivestockLogic.productionMultiplier(genome.productivity(), state.fed(),
                state.ailment() != Ailment.NONE);
        for (ProductionModifier modifier : PRODUCTION_MODIFIERS) {
            value *= modifier.modify(animal);
        }
        return value;
    }

    /** Enregistre un modificateur de production (appelé à l'initialisation). */
    public static void registerProductionModifier(ProductionModifier modifier) {
        PRODUCTION_MODIFIERS.add(modifier);
    }

    /** Enregistre une source de nourriture (appelé à l'initialisation). */
    public static void registerFeedSource(FeedSource source) {
        FEED_SOURCES.add(source);
    }

    /**
     * Cycle de besoins d'un animal (appelé toutes les {@link LivestockLogic#TICK_INTERVAL} ticks par
     * {@link LivestockEvents}, décalé selon l'identifiant pour étaler la charge).
     */
    public static void runNeedsTick(ServerLevel level, Animal animal) {
        boolean winter = SeasonApi.getSeason(level) == Season.WINTER;
        BlockPos below = animal.blockPosition().below();
        boolean onGrass = isGrazer(animal) && level.getBlockState(below).is(Blocks.GRASS_BLOCK);
        long day = SeasonApi.dayIndex(level);
        LivestockState next = LivestockLogic.needsStep(getState(animal), winter, onGrass, day);
        for (FeedSource source : FEED_SOURCES) {
            int added = source.feed(level, animal);
            if (added > 0) {
                next = next.withFed(next.fed() + added).withLastFedDay(day);
            }
        }
        setState(animal, next);
    }

    /** Génome initial si l'animal n'en a pas encore (appelé au chargement serveur). */
    public static void ensureGenome(Animal animal) {
        if (!animal.hasData(LivestockModule.GENOME.get())) {
            animal.setData(LivestockModule.GENOME.get(), GenomeLogic.randomInitial(Math::random));
        }
    }
}
