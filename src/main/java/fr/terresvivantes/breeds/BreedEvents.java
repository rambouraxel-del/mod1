package fr.terresvivantes.breeds;

import fr.terresvivantes.breeds.logic.Breed;
import fr.terresvivantes.breeds.logic.BreedLogic;
import fr.terresvivantes.breeds.logic.Species;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.logic.LivestockState;
import fr.terresvivantes.breeds.network.BreedSyncPayload;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Branchements serveur du module breeds : attribution à l'arrivée, hérédité, synchronisation des suiveurs, viande
 * de Charolais, œuf supplémentaire de Marans, et compensation d'hiver de Highland (écouteur du livestock).
 */
final class BreedEvents {

    /** Rayon de recherche de la poule qui vient de pondre autour de l'œuf lâché. */
    private static final double LAYING_RADIUS = 1.5;

    /** Vrai pendant que l'œuf supplémentaire est ajouté au monde (évite la récursion sur l'événement d'arrivée). */
    private static boolean spawningExtraEgg;

    private BreedEvents() {
    }

    static void register(IEventBus bus) {
        bus.addListener(BreedEvents::onEntityJoin);
        bus.addListener(BreedEvents::onBabySpawn);
        bus.addListener(BreedEvents::onStartTracking);
        bus.addListener(BreedEvents::onLivingDrops);
    }

    /**
     * Attribution au premier chargement côté serveur : vache ou poule sans race reçoit une race selon le climat du
     * biome. Un œuf de poule Marans entré dans le monde peut être doublé.
     */
    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity item) {
            maybeExtraEgg(level, item);
            return;
        }
        Species species = BreedsApi.speciesOf(entity);
        if (species == null || !(entity instanceof Animal animal) || animal.hasData(BreedsModule.BREED.get())) {
            return;
        }
        float temperature = level.getBiome(animal.blockPosition()).value().getBaseTemperature();
        Breed breed = BreedLogic.pick(species, BreedLogic.climate(temperature), level.getRandom().nextDouble());
        animal.setData(BreedsModule.BREED.get(), breed);
        BreedsApi.sync(animal);
    }

    /** Hérédité : la race de l'enfant est celle d'un des parents, à 50/50. */
    private static void onBabySpawn(BabyEntitySpawnEvent event) {
        AgeableMob child = event.getChild();
        if (child == null || !(event.getParentA() instanceof Animal parentA) || !(event.getParentB() instanceof Animal parentB)) {
            return;
        }
        inherit(child, parentA, parentB);
    }

    /**
     * Race d'un enfant (premier bébé ou jumeau) : celle d'un des parents, à 50/50, tirée sur l'enfant. Sans effet pour
     * une espèce sans races.
     */
    static void inherit(Entity child, Animal parentA, Animal parentB) {
        if (BreedsApi.speciesOf(child) == null) {
            return;
        }
        Breed inherited = BreedLogic.inherit(BreedsApi.getBreed(parentA), BreedsApi.getBreed(parentB),
                child.getRandom().nextDouble());
        if (inherited != null) {
            child.setData(BreedsModule.BREED.get(), inherited);
        }
    }

    /** Jumeau livestock : il reçoit lui aussi la race d'un de ses parents (les jumeaux ne passent pas par l'événement d'arrivée). */
    static void onTwin(Animal twin, Animal parentA, Animal parentB) {
        inherit(twin, parentA, parentB); // la synchronisation client se fait au suivi (onStartTracking)
    }

    /** Un joueur qui commence à suivre un animal reçoit sa race. */
    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof Animal animal)) {
            return;
        }
        Breed breed = BreedsApi.getBreed(animal);
        if (breed != null) {
            PacketDistributor.sendToPlayer(player, new BreedSyncPayload(animal.getId(), breed.id()));
        }
    }

    /** Charolais : un morceau de viande de plus à l'abattage (adulte). */
    private static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Animal animal) || animal.isBaby()) {
            return;
        }
        int extra = BreedLogic.extraMeat(BreedsApi.getBreed(animal));
        for (int i = 0; i < extra; i++) {
            event.getDrops().add(new ItemEntity(animal.level(), animal.getX(), animal.getY() + 0.5, animal.getZ(),
                    new ItemStack(Items.BEEF)));
        }
    }

    /**
     * Marans : un œuf lâché par une poule Marans proche peut en donner un second (10 %). L'œuf ajouté repasse par
     * l'événement d'arrivée; le drapeau {@link #spawningExtraEgg} empêche qu'il soit doublé à son tour.
     */
    private static void maybeExtraEgg(ServerLevel level, ItemEntity item) {
        if (spawningExtraEgg || !item.getItem().is(Items.EGG)) {
            return;
        }
        List<Chicken> hens = level.getEntitiesOfClass(Chicken.class, item.getBoundingBox().inflate(LAYING_RADIUS));
        for (Chicken hen : hens) {
            if (BreedLogic.extraEgg(BreedsApi.getBreed(hen), level.getRandom().nextDouble())) {
                ItemEntity extra = new ItemEntity(level, item.getX(), item.getY(), item.getZ(), new ItemStack(Items.EGG));
                spawningExtraEgg = true;
                try {
                    level.addFreshEntity(extra);
                } finally {
                    spawningExtraEgg = false;
                }
                return;
            }
        }
    }

    /**
     * Highland : en hiver, la satiété compensée annule le surcroît de consommation hivernale. Écouteur du cycle de
     * besoins (et non source de nourriture) : {@code lastFedDay} n'est pas modifié, il ne reflète que les vrais repas.
     */
    static void winterCompensation(ServerLevel level, Animal animal, LivestockState state) {
        if (!BreedLogic.cancelsWinterHunger(BreedsApi.getBreed(animal))
                || SeasonApi.getSeason(level) != Season.WINTER) {
            return;
        }
        LivestockApi.setState(animal, state.withFed(LivestockState.clamp(state.fed() + BreedLogic.winterFeedCompensation())));
    }
}
