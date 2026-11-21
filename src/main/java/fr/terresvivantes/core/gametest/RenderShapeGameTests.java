package fr.terresvivantes.core.gametest;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Garde-fou du rendu : tout bloc du mod doit avoir un rendu de modele ({@link RenderShape#MODEL}), sinon il est
 * invisible en jeu. Aucun bloc n'est exclu : un bloc qui se dessine par BlockEntityRenderer seul devrait
 * l'indiquer ici explicitement.
 */
@GameTestHolder(TerresVivantes.MODID)
@PrefixGameTestTemplate(false)
public final class RenderShapeGameTests {

    /** Blocs volontairement non rendus par modele (vide : aucun aujourd'hui). */
    private static final Set<String> EXEMPT = Set.of();

    private RenderShapeGameTests() {
    }

    @GameTest(template = "")
    public static void moddedBlocksUseModelRenderShape(GameTestHelper helper) {
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (!TerresVivantes.MODID.equals(id.getNamespace()) || EXEMPT.contains(id.getPath())) {
                continue;
            }
            checked++;
            RenderShape shape = block.defaultBlockState().getRenderShape();
            if (shape != RenderShape.MODEL) {
                bad.add(id + "=" + shape);
            }
        }
        helper.assertTrue(checked > 0, "aucun bloc terresvivantes trouve");
        helper.assertTrue(bad.isEmpty(), "blocs non rendus par modele: " + bad);
        helper.succeed();
    }
}
