package fr.terresvivantes.vet;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.LivestockState;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.vet.logic.VetLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;

/**
 * Écouteur du cycle de besoins (livestock) : évaluation des maladies et parasites, contagion, effets, sel, mort à
 * santé 0. Appelé à chaque cycle de chaque animal d'élevage.
 */
final class VetNeeds {

    private static final ResourceLocation SICK_SLOW_ID =
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "sick_slow");

    private VetNeeds() {
    }

    static void onNeedsTick(ServerLevel level, Animal animal, LivestockState state) {
        boolean salt = VetApi.isNearSaltLick(level, animal.blockPosition());
        int crowd = level.getEntitiesOfClass(Animal.class, animal.getBoundingBox().inflate(VetLogic.CROWD_RADIUS),
                LivestockApi::isLivestock).size();
        int hardiness = LivestockApi.getGenome(animal).hardiness();

        LivestockState next = state;
        if (next.ailment() == Ailment.NONE) {
            next = evaluateAilment(level, animal, next, crowd, hardiness, salt);
        }
        if (next.ailment() == Ailment.SICK) {
            spreadContagion(level, animal);
            next = VetLogic.sickStep(next);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, animal.getX(), animal.getY() + animal.getBbHeight(),
                    animal.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
        } else if (next.ailment() == Ailment.PARASITES) {
            next = VetLogic.parasiteStep(next);
        }
        if (salt) {
            next = VetLogic.saltStep(next);
        }
        LivestockApi.setState(animal, next);
        updateSickSlowdown(animal, next.ailment() == Ailment.SICK);

        if (VetLogic.shouldKill(LivestockApi.getState(animal), VetConfig.killAtZeroHealth())) {
            animal.hurt(animal.damageSources().generic(), Float.MAX_VALUE);
        }
    }

    /** Un tirage de maladie, puis (si pas malade) un tirage de parasites pour un broutard exposé. */
    private static LivestockState evaluateAilment(ServerLevel level, Animal animal, LivestockState state,
                                                  int crowd, int hardiness, boolean salt) {
        double sick = VetLogic.sickChance(crowd, state.fed(), hardiness, salt);
        if (VetLogic.hits(sick, VetApi.roll())) {
            return state.withAilment(Ailment.SICK);
        }
        BlockPos below = animal.blockPosition().below();
        boolean rainOutdoors = level.isRaining() && level.canSeeSky(animal.blockPosition());
        boolean grassBelow = level.getBlockState(below).is(Blocks.GRASS_BLOCK);
        Season season = SeasonApi.getSeason(level);
        boolean springOrAutumn = season == Season.SPRING || season == Season.AUTUMN;
        if (VetLogic.parasiteExposed(LivestockApi.isGrazer(animal), rainOutdoors, grassBelow, springOrAutumn)
                && VetLogic.hits(VetLogic.parasiteChance(hardiness), VetApi.roll())) {
            return state.withAilment(Ailment.PARASITES);
        }
        return state;
    }

    /** Un animal malade infecte chaque voisin sain du rayon de contagion avec sa chance par cycle. */
    private static void spreadContagion(ServerLevel level, Animal sick) {
        for (Animal neighbour : level.getEntitiesOfClass(Animal.class,
                sick.getBoundingBox().inflate(VetLogic.CONTAGION_RADIUS), LivestockApi::isLivestock)) {
            if (neighbour == sick || LivestockApi.getState(neighbour).ailment() != Ailment.NONE) {
                continue;
            }
            if (VetLogic.contagionHits(VetApi.roll())) {
                LivestockApi.setAilment(neighbour, Ailment.SICK);
            }
        }
    }

    /** Ralentissement de 20 % (modificateur de vitesse, non sauvegardé) tant que l'animal est malade. */
    private static void updateSickSlowdown(Animal animal, boolean sick) {
        AttributeInstance speed = animal.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (sick) {
            if (speed.getModifier(SICK_SLOW_ID) == null) {
                speed.addTransientModifier(new AttributeModifier(SICK_SLOW_ID, VetLogic.SICK_SPEED_MODIFIER,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else {
            speed.removeModifier(SICK_SLOW_ID);
        }
    }
}
