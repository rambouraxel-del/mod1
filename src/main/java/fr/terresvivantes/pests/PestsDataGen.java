package fr.terresvivantes.pests;

import fr.terresvivantes.pests.block.ScarecrowBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;

import java.util.List;
import java.util.function.Function;

/**
 * Modèles et blockstates de l'épouvantail. Deux modèles en éléments pour les moitiés du bloc : la partie basse
 * (poteau, jupe de paille, chemise, bras) et la partie haute (cou, tête en sac de jute, chapeau de paille). Le modèle
 * d'item est autonome (même géométrie que la partie basse) : il ne dépend pas d'un modèle de bloc généré plus tard.
 */
final class PestsDataGen {

    /** Cube d'un élément de modèle, en pixels de 0 à 16, avec la texture logique appliquée sur toutes les faces. */
    private record Box(float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
    }

    private static final List<Box> LOWER = List.of(
            new Box(7, 0, 7, 9, 16, 9, "post"),
            new Box(4, 0, 5, 12, 6, 11, "straw"),
            new Box(5, 6, 6, 11, 13, 10, "cloth"),
            new Box(2, 10, 6, 14, 12, 10, "cloth"),
            new Box(1, 10, 6, 3, 12, 10, "straw"),
            new Box(13, 10, 6, 15, 12, 10, "straw"));

    private static final List<Box> UPPER = List.of(
            new Box(7, 0, 7, 9, 4, 9, "post"),
            new Box(5, 4, 5, 11, 13, 11, "sack"),
            new Box(1, 13, 1, 15, 14, 15, "straw"),
            new Box(4, 14, 4, 12, 16, 12, "straw"));

    private PestsDataGen() {
    }

    static void registerBlockStates(BlockStateProvider provider) {
        BlockModelBuilder lower = blockModel(provider, "scarecrow_lower", LOWER);
        BlockModelBuilder upper = blockModel(provider, "scarecrow_upper", UPPER);
        provider.getVariantBuilder(PestsModule.SCARECROW.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(ScarecrowBlock.HALF) == DoubleBlockHalf.LOWER ? lower : upper)
                .rotationY(yRotation(state.getValue(ScarecrowBlock.FACING)))
                .build());
    }

    static void registerItemModel(ItemModelProvider provider) {
        ItemModelBuilder builder = provider.getBuilder("item/scarecrow").renderType("minecraft:cutout");
        addElements(withTextures(builder, provider::modLoc), LOWER);
    }

    private static BlockModelBuilder blockModel(BlockStateProvider provider, String name, List<Box> boxes) {
        BlockModelBuilder builder = provider.models().getBuilder("block/" + name).renderType("minecraft:cutout");
        addElements(withTextures(builder, provider::modLoc), boxes);
        return builder;
    }

    private static <T extends ModelBuilder<T>> T withTextures(T builder, Function<String, ResourceLocation> location) {
        return builder
                .texture("post", location.apply("block/scarecrow_post"))
                .texture("cloth", location.apply("block/scarecrow_cloth"))
                .texture("straw", location.apply("block/scarecrow_straw"))
                .texture("sack", location.apply("block/scarecrow_sack"));
    }

    private static <T extends ModelBuilder<T>> void addElements(T builder, List<Box> boxes) {
        for (Box box : boxes) {
            var element = builder.element().from(box.x1(), box.y1(), box.z1()).to(box.x2(), box.y2(), box.z2());
            for (Direction direction : Direction.values()) {
                element.face(direction).texture("#" + box.texture()).end();
            }
            element.end();
        }
    }

    /** Rotation du modèle selon l'orientation du bloc (modèle dessiné face au nord). */
    private static int yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
