package fr.terresvivantes.dairy.block;

import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.core.quality.logic.QualityLogic;
import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.dairy.logic.CheeseLogic;
import fr.terresvivantes.processing.ProcessingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity de l'étagère d'affinage.
 *
 * <ul>
 *   <li>Vitesse : la lumière locale règle la progression ({@link CheeseLogic#agingSpeed}). La progression est
 *       ajustée après chaque tick du framework, par un reliquat fractionnaire, sans toucher au framework.</li>
 *   <li>Qualité : le fromage jeune gagne une étoile juste avant la sortie (une seule fois par fromage, mémorisé),
 *       pour que la sortie et sa prévision de qualité restent cohérentes.</li>
 *   <li>Visuel : {@link AgingShelfBlock#HAS_CHEESE} suit la présence d'un fromage.</li>
 * </ul>
 */
public final class AgingShelfBlockEntity extends ProcessingBlockEntity {

    private static final String TAG_RIPENED = "Ripened";
    /** Ticks de progression avant la fin de la recette, à partir desquels le fromage est affiné. */
    private static final int RIPEN_WINDOW = 2;

    /** Vrai une fois le fromage jeune affiné (une seule étoile gagnée par fromage). */
    private boolean ripened;
    /** Reliquat de progression fractionnaire (vitesse différente de 1). */
    private float speedCarry;

    public AgingShelfBlockEntity(BlockPos pos, BlockState state) {
        super(DairyModule.AGING_SHELF_BE.get(), pos, state, DairyModule.AGING_SHELF_TYPE);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack cheese = inventory().getStackInSlot(0);
        if (cheese.isEmpty()) {
            ripened = false;
        } else if (!ripened) {
            ripenIfDue(cheese);
        }
        super.serverTick(level, pos, state);
        if (progress() > 0) {
            applySpeed(level, pos);
        } else {
            speedCarry = 0.0F;
        }
        updateVisual(level, pos);
    }

    /** Fromage jeune proche de sa sortie : il gagne une étoile (règle {@link CheeseLogic#ripenedQuality}). */
    private void ripenIfDue(ItemStack cheese) {
        if (!cheese.is(DairyModule.YOUNG_CHEESE.get()) || totalTime() <= 0
                || progress() + RIPEN_WINDOW < totalTime()) {
            return;
        }
        int base = QualityModule.qualityOf(cheese);
        if (base != QualityLogic.ABSENT) {
            QualityModule.setQuality(cheese, CheeseLogic.ripenedQuality(base));
        }
        ripened = true;
        setChanged();
    }

    /** Le framework ajoute 1 à la progression; on ajoute ou retire le reste pour atteindre la vitesse voulue. */
    private void applySpeed(ServerLevel level, BlockPos pos) {
        boolean sunlit = level.canSeeSky(pos) && level.isDay();
        float speed = CheeseLogic.agingSpeed(level.getMaxLocalRawBrightness(pos), sunlit);
        speedCarry += speed - 1.0F;
        while (speedCarry >= 1.0F) {
            dataAccess().set(0, progress() + 1);
            speedCarry -= 1.0F;
        }
        while (speedCarry <= -1.0F && progress() > 1) {
            dataAccess().set(0, progress() - 1);
            speedCarry += 1.0F;
        }
    }

    private void updateVisual(ServerLevel level, BlockPos pos) {
        boolean hasCheese = !inventory().getStackInSlot(0).isEmpty();
        BlockState now = level.getBlockState(pos);
        if (now.hasProperty(AgingShelfBlock.HAS_CHEESE) && now.getValue(AgingShelfBlock.HAS_CHEESE) != hasCheese) {
            level.setBlock(pos, now.setValue(AgingShelfBlock.HAS_CHEESE, hasCheese), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(TAG_RIPENED, ripened);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ripened = tag.getBoolean(TAG_RIPENED);
    }
}
