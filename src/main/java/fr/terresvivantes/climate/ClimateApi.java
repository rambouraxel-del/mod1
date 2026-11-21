package fr.terresvivantes.climate;

import fr.terresvivantes.climate.logic.ClimateModel;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Facade publique du module climate. */
public final class ClimateApi {

    /** Hauteur maximale, en blocs au-dessus de la culture, où chercher le toit de la serre. */
    public static final int SHELTER_SEARCH_HEIGHT = 12;

    private static final TagKey<Block> GLASS_BLOCKS = commonBlockTag("glass_blocks");
    private static final TagKey<Block> GLASS_PANES = commonBlockTag("glass_panes");

    private ClimateApi() {
    }

    /**
     * Temperature locale en °C. Overworld : biome, saison, altitude et nuit. Autres dimensions : biome et altitude.
     * Un abri sous verre (voir {@link #isSheltered}) ajoute {@link ClimateModel#GREENHOUSE_BONUS}.
     */
    public static float temperatureAt(Level level, BlockPos pos) {
        float biomeBase = level.getBiome(pos).value().getBaseTemperature();
        float temperature;
        if (Level.OVERWORLD.equals(level.dimension())) {
            temperature = ClimateModel.overworldTemperature(
                    biomeBase, SeasonApi.getSeason(level), pos.getY(), level.getDayTime());
        } else {
            temperature = ClimateModel.otherDimensionTemperature(biomeBase, pos.getY());
        }
        return isSheltered(level, pos) ? temperature + ClimateModel.GREENHOUSE_BONUS : temperature;
    }

    /**
     * Vrai si la position est abritée par une serre : en remontant jusqu'à {@link #SHELTER_SEARCH_HEIGHT} blocs
     * au-dessus, le premier bloc non-air est dans {@code c:glass_blocks} ou {@code c:glass_panes}, et la face
     * supérieure de ce verre est à ciel ouvert. Le test porte sur le dessus du verre : le verre lui-même
     * est dans la heightmap et ne voit donc jamais le ciel.
     */
    public static boolean isSheltered(Level level, BlockPos pos) {
        for (int dy = 1; dy <= SHELTER_SEARCH_HEIGHT; dy++) {
            BlockPos above = pos.above(dy);
            BlockState state = level.getBlockState(above);
            if (state.isAir()) {
                continue;
            }
            boolean glass = state.is(GLASS_BLOCKS) || state.is(GLASS_PANES);
            return glass && level.canSeeSky(above.above());
        }
        return false;
    }

    private static TagKey<Block> commonBlockTag(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", name));
    }
}
