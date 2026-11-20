package fr.terresvivantes.automation;

import fr.terresvivantes.automation.logic.CycleLogic;
import fr.terresvivantes.automation.logic.WorkArea;
import fr.terresvivantes.crop.CropApi;
import fr.terresvivantes.orchard.OrchardApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.List;

/**
 * Récolteuse. La zone est un volume de 5x5x3 devant la machine : la case de culture (niveau de la machine) et deux
 * couches au-dessus, pour les feuillages fruitiers. Chaque cycle parcourt au plus {@value #CELLS_PER_CYCLE} cases
 * (curseur incrémental), récolte les cultures mûres via {@link CropApi#harvestAndReplant} et les feuilles fruitières
 * mûres via {@link OrchardApi#harvestRipeLeaves}. Les récoltes vont dans l'inventaire interne, poussé vers le
 * conteneur adjacent derrière ou dessous. Sans place libre, elle s'arrête.
 */
public final class HarvesterBlockEntity extends AutomationBlockEntity {

    /** Cases traitées par cycle : le volume de 75 cases (5x5x3) est parcouru en 15 cycles. */
    static final int CELLS_PER_CYCLE = 5;
    /** Hauteur de la zone, en couches à partir du niveau de la machine. */
    static final int HEIGHT = 3;
    private static final int SLOTS = 9;

    private final IItemHandler internal;
    private final IItemHandler output;

    public HarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(AutomationModule.HARVESTER_BE.get(), pos, state, SLOTS, stack -> true);
        this.internal = new InvWrapper(container());
        this.output = new RestrictedItemHandler(internal, false, true);
    }

    /** Capacité d'extraction : une trémie placée sous la récolteuse peut prendre ses récoltes. */
    public IItemHandler outputHandler() {
        return output;
    }

    @Override
    protected void runCycle(ServerLevel level) {
        if (paused(level)) {
            setActive(level, false);
            return;
        }
        WorkArea area = area();
        int plane = area.cellCount();
        int total = plane * HEIGHT;
        int start = cursor();
        int processed = 0;
        boolean harvested = false;
        while (processed < CELLS_PER_CYCLE && processed < total) {
            if (!container().hasEmptySlot()) {
                break;
            }
            int index = (start + processed) % total;
            BlockPos cell = cellAt(area, index % plane).above(index / plane);
            processed++;
            List<ItemStack> drops = harvestCell(level, cell);
            if (drops.isEmpty()) {
                continue;
            }
            harvested = true;
            for (ItemStack drop : drops) {
                ItemStack rest = ItemHandlerHelper.insertItemStacked(internal, drop, false);
                if (!rest.isEmpty()) {
                    Block.popResource(level, getBlockPos(), rest);
                }
            }
            celebrate(level, cell);
        }
        setCursor(CycleLogic.nextCursor(start, processed, total));
        pushOut(level);
        setActive(level, harvested);
        setChanged();
    }

    /** Récolte une case : culture mûre (replantée) ou feuille fruitière mûre. Liste vide sinon. */
    private static List<ItemStack> harvestCell(ServerLevel level, BlockPos cell) {
        if (CropApi.isMatureCrop(level.getBlockState(cell))) {
            return CropApi.harvestAndReplant(level, cell, null);
        }
        return OrchardApi.harvestRipeLeaves(level, cell);
    }

    /** Pousse le contenu vers le conteneur derrière, puis dessous. Le reste reste dans la machine. */
    private void pushOut(ServerLevel level) {
        BlockState state = getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        for (Direction toward : new Direction[]{facing.getOpposite(), Direction.DOWN}) {
            BlockPos target = getBlockPos().relative(toward);
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, toward.getOpposite());
            if (handler == null) {
                continue;
            }
            for (int slot = 0; slot < container().getContainerSize(); slot++) {
                ItemStack stack = container().getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                container().setItem(slot, ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false));
            }
            if (container().isEmpty()) {
                return;
            }
        }
    }

    private void celebrate(ServerLevel level, BlockPos cell) {
        level.playSound(null, cell, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.6f, 1.0f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5,
                6, 0.3, 0.2, 0.3, 0.0);
    }
}
