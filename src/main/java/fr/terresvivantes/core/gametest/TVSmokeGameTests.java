package fr.terresvivantes.core.gametest;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Test de fumee: le serveur gametest refuse de demarrer sans aucune fonction de test.
 * Ce test verifie seulement que le mod se charge; il sera remplace par de vrais tests.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class TVSmokeGameTests {

    private TVSmokeGameTests() {
    }

    @GameTest(template = "")
    public static void modLoads(GameTestHelper helper) {
        helper.succeed();
    }
}
