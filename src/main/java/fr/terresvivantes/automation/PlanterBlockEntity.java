package fr.terresvivantes.automation;

import fr.terresvivantes.automation.logic.CycleLogic;
import fr.terresvivantes.automation.logic.PlanterLogic;
import fr.terresvivantes.automation.logic.WorkArea;
import fr.terresvivantes.crop.CropApi;
import fr.terresvivantes.soil.SoilApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * Planteuse. Chaque cycle plante au plus une graine, sur une terre labourée de la zone qui a de l'air au-dessus
 * (la zone est au même niveau Y que la machine, la terre est juste dessous). Les graines tournent entre les cases
 * d'inventaire, et une graine différente de la dernière culture de la terre est préférée quand elle existe.
 * L'inventaire n'accepte que des graines et se remplit depuis une trémie ou la main.
 */
public final class PlanterBlockEntity extends AutomationBlockEntity {

    private static final int SLOTS = 9;
    private static final String TAG_ROTATION = "Rotation";

    private final IItemHandler insertHandler;
    private int rotation;

    public PlanterBlockEntity(BlockPos pos, BlockState state) {
        super(AutomationModule.PLANTER_BE.get(), pos, state, SLOTS, PlanterBlockEntity::isSeed);
        this.insertHandler = new RestrictedItemHandler(new InvWrapper(container()), true, false);
    }

    /** Vrai pour une graine : un objet qui pose une culture. */
    public static boolean isSeed(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock;
    }

    /** Capacité d'insertion : une trémie peut y verser des graines (jamais en extraire). */
    public IItemHandler insertHandler() {
        return insertHandler;
    }

    @Override
    protected void runCycle(ServerLevel level) {
        if (paused(level)) {
            setActive(level, false);
            return;
        }
        WorkArea area = area();
        int total = area.cellCount();
        int start = cursor();
        String[] crops = seedCrops();
        boolean planted = false;
        for (int i = 0; i < total && !planted; i++) {
            int index = (start + i) % total;
            BlockPos cell = cellAt(area, index);
            BlockPos soil = cell.below();
            if (!level.getBlockState(soil).is(Blocks.FARMLAND) || !level.getBlockState(cell).isAir()) {
                continue;
            }
            int slot = PlanterLogic.chooseSeedSlot(crops, rotation, SoilApi.getLastCrop(level, soil));
            if (slot < 0) {
                break;
            }
            if (plant(level, soil, slot)) {
                rotation = (slot + 1) % SLOTS;
                setCursor(CycleLogic.nextCursor(start, i + 1, total));
                planted = true;
            }
        }
        setActive(level, planted);
        setChanged();
    }

    /** Identifiant de culture de chaque case de graines (null si vide ou non plantable). */
    private String[] seedCrops() {
        String[] crops = new String[container().getContainerSize()];
        for (int slot = 0; slot < crops.length; slot++) {
            ItemStack stack = container().getItem(slot);
            if (isSeed(stack)) {
                BlockState planted = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
                crops[slot] = CropApi.cropIdOf(planted);
            }
        }
        return crops;
    }

    /** Pose une graine de la case donnée sur la terre; retire une graine de la case en cas de succès. */
    private boolean plant(ServerLevel level, BlockPos soil, int slot) {
        ItemStack seed = container().getItem(slot);
        if (!(seed.getItem() instanceof BlockItem item)) {
            return false;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
        BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND,
                seed.copyWithCount(1), hit);
        InteractionResult result = item.place(context);
        if (!result.consumesAction()) {
            return false;
        }
        seed.shrink(1);
        container().setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_ROTATION, rotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rotation = tag.getInt(TAG_ROTATION);
    }
}
