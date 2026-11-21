package fr.terresvivantes.guide.gametest;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.guide.GuideEvents;
import fr.terresvivantes.guide.GuideModule;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests de jeu du guide : remise du manuel à la première connexion. */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class GuideGameTests {

    private GuideGameTests() {
    }

    /** Un joueur qui se connecte deux fois ne reçoit le manuel qu'une seule fois. */
    @GameTest(template = "modloads")
    public static void manualIsGivenOnlyOnce(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!player.getData(GuideModule.MANUAL_GIVEN.get()), "le drapeau doit être faux avant la connexion");

        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        GuideEvents.grantFirstManual(player);

        int count = player.getInventory().countItem(GuideModule.FARMERS_MANUAL.get());
        helper.assertTrue(count == 1, "un seul manuel attendu, trouvé " + count);
        helper.assertTrue(player.getData(GuideModule.MANUAL_GIVEN.get()), "le drapeau manual_given doit être posé");
        helper.succeed();
    }
}
