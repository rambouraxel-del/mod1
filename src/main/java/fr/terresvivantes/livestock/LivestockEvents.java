package fr.terresvivantes.livestock;

import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.livestock.logic.Genome;
import fr.terresvivantes.livestock.logic.GenomeLogic;
import fr.terresvivantes.livestock.logic.LivestockLogic;
import fr.terresvivantes.livestock.logic.LivestockState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Branchements serveur du module livestock : génome à l'arrivée, cycle de besoins, hérédité et jumeaux,
 * refus de reproduction, ponte modulée par la productivité.
 */
final class LivestockEvents {

    private LivestockEvents() {
    }

    static void register(IEventBus bus) {
        bus.addListener(LivestockEvents::onEntityJoin);
        bus.addListener(LivestockEvents::onEntityTick);
        bus.addListener(LivestockEvents::onBabySpawn);
        bus.addListener(LivestockEvents::onEntityInteract);
    }

    /** Génome initial au premier chargement côté serveur (un animal déjà sauvegardé garde le sien). */
    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Animal animal)
                || !LivestockApi.isLivestock(animal)) {
            return;
        }
        LivestockApi.ensureGenome(animal);
    }

    /**
     * Un cycle de besoins toutes les {@link LivestockLogic#TICK_INTERVAL} ticks, décalé par identifiant. La ponte
     * des poules est accélérée (ou ralentie) par lot au même rythme, selon la productivité.
     */
    private static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Animal animal) || !(animal.level() instanceof ServerLevel level)
                || !LivestockApi.isLivestock(animal)) {
            return;
        }
        if (animal.tickCount % LivestockLogic.TICK_INTERVAL == Math.floorMod(animal.getId(), LivestockLogic.TICK_INTERVAL)) {
            LivestockApi.runNeedsTick(level, animal);
            if (animal instanceof Chicken chicken && !chicken.isBaby()) {
                applyEggRate(chicken);
            }
        }
    }

    /**
     * Ponte : vanilla décrémente {@code eggTime} d'un tick; on ajoute (multiplicateur − 1) × 1200 ticks en un lot,
     * ce qui donne une cadence de ponte égale au multiplicateur de production.
     */
    private static void applyEggRate(Chicken chicken) {
        float multiplier = LivestockApi.productionMultiplier(chicken);
        double extra = (multiplier - 1.0) * LivestockLogic.TICK_INTERVAL;
        chicken.eggTime -= LivestockLogic.roundProbabilistic(extra, ThreadLocalRandom.current()::nextDouble);
    }

    /**
     * Héritage : génome de l'enfant tiré des parents. Un second bébé apparaît avec la chance de fertilité × 3 %
     * (moyenne des deux parents), au même endroit, avec son propre génome hérité.
     */
    private static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof Animal parentA) || !(event.getParentB() instanceof Animal parentB)
                || !LivestockApi.isLivestock(parentA)) {
            return;
        }
        AgeableMob child = event.getChild();
        if (child == null) {
            return;
        }
        Genome genomeA = LivestockApi.getGenome(parentA);
        Genome genomeB = LivestockApi.getGenome(parentB);
        var random = ThreadLocalRandom.current();
        child.setData(LivestockModule.GENOME.get(), GenomeLogic.inherit(genomeA, genomeB, random::nextDouble));

        double fertility = (genomeA.fertility() + genomeB.fertility()) / 2.0;
        if (parentA.level() instanceof ServerLevel level && random.nextDouble() < LivestockLogic.twinChance(fertility)) {
            AgeableMob twin = parentA.getBreedOffspring(level, parentB);
            if (twin != null) {
                twin.setBaby(true);
                twin.setPos(parentA.getX(), parentA.getY(), parentA.getZ());
                twin.setData(LivestockModule.GENOME.get(), GenomeLogic.inherit(genomeA, genomeB, random::nextDouble));
                if (twin instanceof Animal twinAnimal) {
                    LivestockApi.notifyTwin(twinAnimal, parentA, parentB);
                }
                level.addFreshEntity(twin);
            }
        }
    }

    /**
     * Refus de reproduction : la nourriture de reproduction n'est acceptée que si la satiété atteint 40 et, selon la
     * config, au printemps ou en été. Le refus annule l'interaction et affiche un message dans la barre d'action.
     */
    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Animal animal) || !LivestockApi.isLivestock(animal)
                || !(animal.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!animal.isFood(stack) || !animal.canFallInLove()) {
            return;
        }
        Season season = SeasonApi.getSeason(level);
        boolean springOrSummer = season == Season.SPRING || season == Season.SUMMER;
        int fed = LivestockApi.getState(animal).fed();
        if (LivestockLogic.canBreed(fed, springOrSummer, LivestockConfig.breedingSeasonsOnly())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            Component message = fed < LivestockLogic.BREEDING_MIN_FED
                    ? Component.translatable("message.terresvivantes.livestock.breeding.too_hungry")
                    : Component.translatable("message.terresvivantes.livestock.breeding.wrong_season");
            serverPlayer.displayClientMessage(message, true);
        }
    }
}
