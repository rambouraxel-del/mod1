package fr.terresvivantes.automation;

import fr.terresvivantes.automation.logic.CycleLogic;
import fr.terresvivantes.automation.logic.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.function.Predicate;

/**
 * BlockEntity commune des machines automatiques. Le ticker appelle {@link #serverTick}; sans configuration activée,
 * rien n'est fait (aucun scan). Un cycle n'a lieu que tous les {@link AutomationConfig#intervalTicks()} ticks, et la
 * zone de travail est bornée par {@link AutomationConfig#radius()}. Les machines sont à l'arrêt quand un signal
 * redstone les alimente.
 */
public abstract class AutomationBlockEntity extends BlockEntity {

    private static final String TAG_TIMER = "Timer";
    private static final String TAG_CURSOR = "Cursor";

    private final AutomationInventory container;
    private int timer;
    private int cursor;

    protected AutomationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots,
                                    Predicate<ItemStack> accepts) {
        super(type, pos, state);
        this.container = new AutomationInventory(slots, accepts, this::setChanged);
    }

    /** Inventaire interne (menu et capacités). */
    public AutomationInventory container() {
        return container;
    }

    /** Appelé à chaque tick serveur par le bloc. */
    public void serverTick(ServerLevel level) {
        if (!AutomationConfig.enabled()) {
            return;
        }
        timer++;
        if (!CycleLogic.isCycleDue(timer, AutomationConfig.intervalTicks())) {
            return;
        }
        timer = 0;
        runCycle(level);
    }

    /** Un cycle de travail de la machine. */
    protected abstract void runCycle(ServerLevel level);

    /** Vrai si la machine est alimentée en redstone (pause). */
    protected boolean paused(ServerLevel level) {
        return level.hasNeighborSignal(getBlockPos());
    }

    /** Zone de travail courante, d'après la configuration. */
    protected WorkArea area() {
        return new WorkArea(AutomationConfig.radius());
    }

    /** Position monde d'une case de la zone : devant la machine, au même niveau Y. */
    protected BlockPos cellAt(WorkArea area, int index) {
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        return getBlockPos()
                .relative(facing, area.forwardOf(index))
                .relative(facing.getClockWise(), area.lateralOf(index));
    }

    protected int cursor() {
        return cursor;
    }

    protected void setCursor(int cursor) {
        this.cursor = cursor;
    }

    /** Met à jour le visuel « actif » sans toucher au reste de l'état. */
    protected void setActive(ServerLevel level, boolean active) {
        BlockState state = getBlockState();
        if (state.getValue(AutomationBlock.ACTIVE) != active) {
            level.setBlock(getBlockPos(), state.setValue(AutomationBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_TIMER, timer);
        tag.putInt(TAG_CURSOR, cursor);
        ContainerHelper.saveAllItems(tag, container.items(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        timer = tag.getInt(TAG_TIMER);
        cursor = tag.getInt(TAG_CURSOR);
        ContainerHelper.loadAllItems(tag, container.items(), registries);
    }
}
