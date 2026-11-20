package fr.terresvivantes.insects.client;

import fr.terresvivantes.insects.InsectsConfig;
import fr.terresvivantes.insects.InsectsModule;
import fr.terresvivantes.insects.logic.FireflyLogic;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Apparition des lucioles, côté client seulement : nuit, été ou fin de printemps. Les lucioles naissent près du
 * joueur, sur l'herbe ou près de l'eau, sans aucun coût serveur.
 */
public final class FireflySpawner {

    /** Lucioles possibles autour du joueur, horizontalement, en blocs. */
    private static final int SPREAD = FireflyLogic.RADIUS;
    /** Hauteur possible au-dessus du joueur, en blocs. */
    private static final int HEIGHT = 4;
    /** Distance horizontale (blocs) à laquelle il faut de l'eau pour attirer une luciole sans herbe. */
    private static final int WATER_RANGE = 2;

    private FireflySpawner() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }
        Season season = SeasonApi.getSeason(level);
        boolean night = FireflyLogic.isNight(level.getDayTime());
        if (!FireflyLogic.isFireflyTime(season, night, SeasonApi.getDayOfSeason(level),
                SeasonApi.getDaysPerSeason())) {
            return;
        }
        RandomSource random = level.getRandom();
        if (!FireflyLogic.shouldSpawn(FireflyParticle.active(), InsectsConfig.fireflyMax(), random.nextDouble())) {
            return;
        }
        BlockPos origin = player.blockPosition();
        BlockPos pos = origin.offset(random.nextInt(2 * SPREAD + 1) - SPREAD,
                random.nextInt(HEIGHT), random.nextInt(2 * SPREAD + 1) - SPREAD);
        if (!level.isLoaded(pos) || !attractive(level, pos)) {
            return;
        }
        level.addParticle(InsectsModule.FIREFLY.get(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
    }

    /** Sur l'herbe, ou près de l'eau. */
    private static boolean attractive(ClientLevel level, BlockPos pos) {
        if (level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)
                || level.getBlockState(pos.below()).is(BlockTags.FLOWERS)) {
            return true;
        }
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-WATER_RANGE, -1, -WATER_RANGE),
                pos.offset(WATER_RANGE, 1, WATER_RANGE))) {
            FluidState fluid = level.getFluidState(near);
            if (fluid.is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
