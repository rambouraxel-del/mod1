package fr.terresvivantes.pests.block;

import fr.terresvivantes.pests.PestsModule;
import fr.terresvivantes.pests.logic.ScarecrowLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Données légères de l'épouvantail : jour de pose (jours de Minecraft). Porté par la partie basse seulement.
 */
public final class ScarecrowBlockEntity extends BlockEntity {

    private static final String TAG_PLACED_DAY = "PlacedDay";

    private long placedDay = ScarecrowLogic.UNKNOWN_DAY;

    public ScarecrowBlockEntity(BlockPos pos, BlockState state) {
        super(PestsModule.SCARECROW_BE.get(), pos, state);
    }

    /** Jour de pose, ou {@link ScarecrowLogic#UNKNOWN_DAY} si inconnu. */
    public long placedDay() {
        return placedDay;
    }

    public void setPlacedDay(long day) {
        this.placedDay = day;
        setChanged();
    }

    /** Fixe le jour de pose à {@code today} s'il est inconnu, puis le renvoie. */
    public long ensurePlacedDay(long today) {
        if (placedDay == ScarecrowLogic.UNKNOWN_DAY) {
            placedDay = today;
            setChanged();
        }
        return placedDay;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (placedDay != ScarecrowLogic.UNKNOWN_DAY) {
            tag.putLong(TAG_PLACED_DAY, placedDay);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        placedDay = tag.contains(TAG_PLACED_DAY) ? tag.getLong(TAG_PLACED_DAY) : ScarecrowLogic.UNKNOWN_DAY;
    }
}
