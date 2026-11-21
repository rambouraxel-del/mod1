package fr.terresvivantes.breeds.logic;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreedLogicTest {

    @Test
    void climateFollowsBiomeTemperature() {
        assertEquals(BreedLogic.Climate.COLD, BreedLogic.climate(0.0F));
        assertEquals(BreedLogic.Climate.TEMPERATE, BreedLogic.climate(0.8F));
        assertEquals(BreedLogic.Climate.WARM, BreedLogic.climate(2.0F));
    }

    @Test
    void coldCowsPickHighlandAtTheRightRolls() {
        // Vaches par froid : HOLSTEIN 2, HIGHLAND 6, CHAROLAIS 2 (total 10).
        assertEquals(Breed.HOLSTEIN, BreedLogic.pick(Species.COW, BreedLogic.Climate.COLD, 0.0));
        assertEquals(Breed.HIGHLAND, BreedLogic.pick(Species.COW, BreedLogic.Climate.COLD, 0.5));
        assertEquals(Breed.CHAROLAIS, BreedLogic.pick(Species.COW, BreedLogic.Climate.COLD, 0.95));
    }

    @Test
    void pickStaysWithinTheSpeciesAndHandlesRollOne() {
        for (int i = 0; i <= 100; i++) {
            Breed picked = BreedLogic.pick(Species.CHICKEN, BreedLogic.Climate.WARM, i / 100.0);
            assertEquals(Species.CHICKEN, picked.species());
        }
        // Un tirage hors bornes (1.0) reste dans l'espèce : on prend la dernière race.
        assertEquals(Species.CHICKEN, BreedLogic.pick(Species.CHICKEN, BreedLogic.Climate.TEMPERATE, 1.0).species());
    }

    @Test
    void coldFavoursSussexAndWarmFavoursLeghornAmongChickens() {
        int coldSussex = countOf(Breed.SUSSEX, Species.CHICKEN, BreedLogic.Climate.COLD);
        int warmSussex = countOf(Breed.SUSSEX, Species.CHICKEN, BreedLogic.Climate.WARM);
        int warmLeghorn = countOf(Breed.LEGHORN, Species.CHICKEN, BreedLogic.Climate.WARM);
        int coldLeghorn = countOf(Breed.LEGHORN, Species.CHICKEN, BreedLogic.Climate.COLD);
        assertTrue(coldSussex > warmSussex);
        assertTrue(warmLeghorn > coldLeghorn);
    }

    @Test
    void childTakesOneParentBreedAtFiftyFifty() {
        assertEquals(Breed.HOLSTEIN, BreedLogic.inherit(Breed.HOLSTEIN, Breed.CHAROLAIS, 0.2));
        assertEquals(Breed.CHAROLAIS, BreedLogic.inherit(Breed.HOLSTEIN, Breed.CHAROLAIS, 0.7));
        assertEquals(Breed.CHAROLAIS, BreedLogic.inherit(null, Breed.CHAROLAIS, 0.1));
        assertEquals(Breed.HOLSTEIN, BreedLogic.inherit(Breed.HOLSTEIN, null, 0.9));
        assertNull(BreedLogic.inherit(null, null, 0.5));
    }

    @Test
    void geneticEffectsOnProductionAndMeat() {
        assertEquals(1.2F, BreedLogic.productionMultiplier(Breed.HOLSTEIN), 1e-6F);
        assertEquals(0.9F, BreedLogic.productionMultiplier(Breed.CHAROLAIS), 1e-6F);
        assertEquals(1.25F, BreedLogic.productionMultiplier(Breed.LEGHORN), 1e-6F);
        assertEquals(1.0F, BreedLogic.productionMultiplier(Breed.SUSSEX), 1e-6F);
        assertEquals(1.0F, BreedLogic.productionMultiplier(null), 1e-6F);

        assertTrue(BreedLogic.extraMeat(Breed.CHAROLAIS) > BreedLogic.extraMeat(Breed.HOLSTEIN));
        assertEquals(0, BreedLogic.extraMeat(Breed.HIGHLAND));
    }

    @Test
    void maransExtraEggHappensOnlyBelowTenPercent() {
        assertTrue(BreedLogic.extraEgg(Breed.MARANS, 0.05));
        assertFalse(BreedLogic.extraEgg(Breed.MARANS, 0.10));
        assertFalse(BreedLogic.extraEgg(Breed.LEGHORN, 0.0));
        assertFalse(BreedLogic.extraEgg(null, 0.0));
    }

    @Test
    void highlandCompensatesWinterHungerExactly() {
        // Consommation hivernale 12, normale 8 : la compensation vaut 4.
        assertEquals(4, BreedLogic.winterFeedCompensation());
        assertTrue(BreedLogic.cancelsWinterHunger(Breed.HIGHLAND));
        assertFalse(BreedLogic.cancelsWinterHunger(Breed.HOLSTEIN));
    }

    @Test
    void breedIdsRoundTripAndRejectUnknown() {
        for (Breed breed : Breed.values()) {
            assertEquals(Optional.of(breed), Breed.byId(breed.id()));
        }
        assertEquals(Optional.of(Breed.SUSSEX), Breed.byId("SUSSEX"));
        assertEquals(Optional.empty(), Breed.byId("zebu"));
        assertEquals(Optional.empty(), Breed.byId(null));
        assertEquals(3, Breed.ofSpecies(Species.COW).size());
        assertEquals(3, Breed.ofSpecies(Species.CHICKEN).size());
    }

    private static int countOf(Breed wanted, Species species, BreedLogic.Climate climate) {
        int count = 0;
        for (int i = 0; i < 1000; i++) {
            if (BreedLogic.pick(species, climate, (i + 0.5) / 1000.0) == wanted) {
                count++;
            }
        }
        return count;
    }
}
