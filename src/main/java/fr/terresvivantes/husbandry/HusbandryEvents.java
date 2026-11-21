package fr.terresvivantes.husbandry;

import fr.terresvivantes.husbandry.logic.FeedLogic;
import fr.terresvivantes.husbandry.logic.FeedType;
import fr.terresvivantes.husbandry.logic.NestLogic;
import fr.terresvivantes.husbandry.logic.Species;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.logic.LivestockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.phys.Vec3;
import fr.terresvivantes.duck.DuckModule;
import fr.terresvivantes.duck.entity.Duck;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collection;

/**
 * Branchements serveur du module husbandry : alimentation des animaux par les auges (source du module livestock) et
 * interception de la ponte des poules vers les nichoirs.
 */
final class HusbandryEvents {

    private HusbandryEvents() {
    }

    static void register(IEventBus bus) {
        bus.addListener(HusbandryEvents::onEntityTickPre);
        LivestockApi.registerFeedSource(HusbandryEvents::feedFromTrough);
    }

    /**
     * Ponte : avant le tick, une poule ou un canard adulte dont le décompte va atteindre 0 pond. Si un nichoir proche
     * peut recevoir l'œuf, il y entre au lieu de tomber au sol, et le délai repart d'un nouveau tirage. La productivité
     * (cadence) reste gérée par le module livestock (poules) ou par le canard lui-même.
     */
    private static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof Chicken chicken) {
            if (NestLogic.shouldIntercept(chicken.eggTime, chicken.isBaby())
                    && layInNest(level, chicken.position(), new ItemStack(Items.EGG))) {
                chicken.eggTime = nextEggTime(level);
            }
        } else if (event.getEntity() instanceof Duck duck) {
            if (NestLogic.shouldIntercept(duck.getEggTime(), duck.isBaby())
                    && layInNest(level, duck.position(), new ItemStack(DuckModule.DUCK_EGG.get()))) {
                duck.setEggTime(nextEggTime(level));
            }
        }
    }

    /** Dépose l'œuf dans le nichoir le plus proche qui peut le recevoir. Vrai si l'œuf est entré. */
    private static boolean layInNest(ServerLevel level, Vec3 position, ItemStack egg) {
        NestingBoxBlockEntity nest = HusbandryRegistry.nearestNestWithRoom(level, position, HusbandryConfig.nestingRadius());
        return nest != null && nest.addEgg(egg);
    }

    private static int nextEggTime(ServerLevel level) {
        return NestLogic.nextEggTime(level.getRandom().nextInt(NestLogic.EGG_TIME_SPREAD));
    }

    /**
     * Source de nourriture (livestock) : un animal qui a faim mange dans l'auge non vide la plus proche qui lui convient.
     * Consomme une unité, renvoie les points de satiété; particules et son côté serveur.
     */
    private static int feedFromTrough(ServerLevel level, Animal animal) {
        LivestockState state = LivestockApi.getState(animal);
        if (!FeedLogic.wantsFood(state.fed())) {
            return 0;
        }
        Species species = speciesOf(animal);
        double radiusSquared = (double) HusbandryConfig.troughRadius() * HusbandryConfig.troughRadius();
        FeedingTroughBlockEntity best = null;
        int bestPoints = 0;
        double bestDistance = Double.MAX_VALUE;
        Collection<FeedingTroughBlockEntity> troughs = HusbandryRegistry.troughs(level);
        for (FeedingTroughBlockEntity trough : troughs) {
            if (trough.isEmpty() || trough.isRemoved()) {
                continue;
            }
            double distance = trough.getBlockPos().getCenter().distanceToSqr(animal.position());
            if (distance > radiusSquared || distance >= bestDistance) {
                continue;
            }
            int points = FeedLogic.points(species, trough.type());
            if (points <= 0) {
                continue;
            }
            best = trough;
            bestPoints = points;
            bestDistance = distance;
        }
        if (best == null) {
            return 0;
        }
        FeedType type = best.type();
        best.consumeUnit();
        double x = animal.getX();
        double y = animal.getY() + animal.getBbHeight() * 0.6;
        double z = animal.getZ();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(HusbandryModule.feedItem(type))),
                x, y, z, 4, 0.15, 0.05, 0.15, 0.02);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.6F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        return bestPoints;
    }

    /** Espèce d'un animal pour l'alimentation. */
    static Species speciesOf(Animal animal) {
        EntityType<?> type = animal.getType();
        if (type == EntityType.COW || type == EntityType.MOOSHROOM) {
            return Species.COW;
        }
        if (type == EntityType.SHEEP) {
            return Species.SHEEP;
        }
        if (type == EntityType.GOAT) {
            return Species.GOAT;
        }
        if (type == EntityType.PIG) {
            return Species.PIG;
        }
        if (type == EntityType.CHICKEN) {
            return Species.CHICKEN;
        }
        if (type == DuckModule.DUCK.get()) {
            return Species.DUCK;
        }
        return Species.OTHER;
    }
}
