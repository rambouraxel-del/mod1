package fr.terresvivantes.processing;

import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.core.quality.logic.QualityLogic;
import fr.terresvivantes.guild.GuildApi;
import fr.terresvivantes.processing.logic.ProcessingLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * BlockEntity commune des machines de transformation : entrées, conteneur optionnel, sortie, progression et recette
 * courante (mise en cache). Le ticker serveur sort tout de suite si les entrées sont vides, et ne cherche une recette
 * que lorsque le contenu a changé. Les modules étendent cette classe et surchargent les hooks
 * {@link #craftingTick} et {@link #craftFinished} pour les sons et particules.
 */
public abstract class ProcessingBlockEntity extends BlockEntity {

    private static final String TAG_INVENTORY = "Inventory";
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_TOTAL = "TotalTime";
    private static final String TAG_LAST_USER = "LastUser";
    /** Sauvegarde de la progression toutes les N ticks de progression (la fin de craft sauvegarde aussi). */
    private static final int SAVE_EVERY = 20;

    private final MachineType machine;
    private final ProcessingInventory inventory;
    private final IItemHandler top;
    private final IItemHandler bottom;
    @Nullable
    private final IItemHandler sides;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> totalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            } else if (index == 1) {
                totalTime = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    private int progress;
    private int totalTime;
    @Nullable
    private RecipeHolder<ProcessingRecipe> recipe;
    private boolean recipeDirty = true;
    /** Dernier joueur ayant ouvert la machine : son titre de guilde donne le bonus de qualité. */
    @Nullable
    private UUID lastUser;

    protected ProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineType machine) {
        super(type, pos, state);
        this.machine = machine;
        this.inventory = new ProcessingInventory(machine, this::inventoryChanged);
        int[] inputs = new int[machine.inputSlots()];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = i;
        }
        this.top = new ProcessingSideHandler(inventory, inputs, new int[0]);
        this.bottom = new ProcessingSideHandler(inventory, new int[0], new int[]{machine.outputSlot()});
        this.sides = machine.container()
                ? new ProcessingSideHandler(inventory, new int[]{machine.containerSlot()}, new int[0])
                : null;
    }

    public MachineType machine() {
        return machine;
    }

    /** Inventaire interne (entrées, conteneur, sortie). */
    public ProcessingInventory inventory() {
        return inventory;
    }

    /** Vue d'une face : haut = entrées, bas = sortie, côtés = conteneur (absent sans conteneur). */
    @Nullable
    public IItemHandler itemHandler(@Nullable Direction side) {
        if (side == null) {
            return inventory;
        }
        return switch (side) {
            case UP -> top;
            case DOWN -> bottom;
            default -> sides;
        };
    }

    /** Données de synchronisation (progression, durée) pour le menu. */
    public ContainerData dataAccess() {
        return data;
    }

    public int progress() {
        return progress;
    }

    public int totalTime() {
        return totalTime;
    }

    /** Appelé par le ticker serveur à chaque tick. */
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (inventory.isInputEmpty()) {
            if (progress != 0 || totalTime != 0) {
                progress = 0;
                totalTime = 0;
            }
            recipe = null;
            setActive(level, pos, false);
            return;
        }
        ProcessingRecipeInput input = input();
        if (recipeDirty || (recipe != null && !recipe.value().matches(input, level))) {
            findRecipe(level, input);
        }
        if (recipe == null) {
            progress = 0;
            setActive(level, pos, false);
            return;
        }
        ProcessingRecipe current = recipe.value();
        ItemStack preview = current.assemble(input, level.registryAccess());
        if (preview.isEmpty() || !outputAccepts(preview)) {
            setActive(level, pos, false);
            return;
        }
        totalTime = current.time();
        progress++;
        setActive(level, pos, true);
        craftingTick(level, pos);
        if (ProcessingLogic.isComplete(progress, totalTime)) {
            List<ItemStack> leftovers = new ArrayList<>();
            ItemStack made = applyGuildBonus(level, current.craft(input, level.registryAccess(), leftovers));
            cleanInputs();
            addOutput(made);
            placeLeftovers(level, pos, leftovers);
            progress = 0;
            craftFinished(level, pos, current);
            setChanged();
        } else if (progress % SAVE_EVERY == 0) {
            setChanged();
        }
    }

    /** Mémorise le joueur qui vient d'interagir avec la machine (ouverture du menu). */
    public void rememberUser(Player player) {
        if (!player.getUUID().equals(lastUser)) {
            lastUser = player.getUUID();
            setChanged();
        }
    }

    /**
     * Bonus de guilde : si le dernier utilisateur a le titre de la filière de cette machine, la sortie passe à la
     * qualité supérieure (plafonnée à 3) avec la probabilité donnée par {@link GuildApi}. Sans qualité, rien.
     */
    private ItemStack applyGuildBonus(ServerLevel level, ItemStack made) {
        if (lastUser == null || made.isEmpty()) {
            return made;
        }
        Player player = level.getPlayerByUUID(lastUser);
        if (player == null) {
            return made;
        }
        float chance = GuildApi.machineQualityBonusChance(player, machine.id());
        int quality = QualityModule.qualityOf(made);
        if (chance > 0f && quality >= QualityLogic.MIN && quality < QualityLogic.MAX
                && level.getRandom().nextFloat() < chance) {
            QualityModule.setQuality(made, quality + 1);
        }
        return made;
    }

    private void findRecipe(ServerLevel level, ProcessingRecipeInput input) {
        ResourceLocation before = recipe == null ? null : recipe.id();
        recipe = level.getRecipeManager()
                .getRecipeFor(ProcessingModule.RECIPE_TYPE.get(), input, level)
                .orElse(null);
        recipeDirty = false;
        if (!Objects.equals(before, recipe == null ? null : recipe.id())) {
            progress = 0;
        }
    }

    private ProcessingRecipeInput input() {
        List<ItemStack> inputs = new ArrayList<>(machine.inputSlots());
        for (int slot = 0; slot < machine.inputSlots(); slot++) {
            inputs.add(inventory.getStackInSlot(slot));
        }
        ItemStack container = machine.container() ? inventory.getStackInSlot(machine.containerSlot()) : ItemStack.EMPTY;
        return new ProcessingRecipeInput(machine.id(), inputs, container);
    }

    private boolean outputAccepts(ItemStack made) {
        ItemStack current = inventory.getStackInSlot(machine.outputSlot());
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, made)
                && current.getCount() + made.getCount() <= current.getMaxStackSize();
    }

    private void addOutput(ItemStack made) {
        if (made.isEmpty()) {
            return;
        }
        ItemStack current = inventory.getStackInSlot(machine.outputSlot());
        if (current.isEmpty()) {
            inventory.setStackInSlot(machine.outputSlot(), made);
        } else {
            current.grow(made.getCount());
        }
    }

    /** Retire les stacks vidés par la recette, pour que les entrées vides soient réellement vides. */
    private void cleanInputs() {
        for (int slot = 0; slot < machine.outputSlot(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getCount() <= 0) {
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Rend les restes des ingrédients consommés (seau vide, etc.) : dans le slot conteneur si libre, sinon dans la
     * sortie si elle accepte le reste, sinon éjectés au-dessus de la machine.
     */
    private void placeLeftovers(ServerLevel level, BlockPos pos, List<ItemStack> leftovers) {
        for (ItemStack leftover : leftovers) {
            ItemStack rest = leftover.copy();
            if (machine.container() && inventory.getStackInSlot(machine.containerSlot()).isEmpty()) {
                inventory.setStackInSlot(machine.containerSlot(), rest);
                continue;
            }
            ItemStack output = inventory.getStackInSlot(machine.outputSlot());
            if (output.isEmpty()) {
                inventory.setStackInSlot(machine.outputSlot(), rest);
                continue;
            }
            if (ItemStack.isSameItemSameComponents(output, rest)
                    && output.getCount() + rest.getCount() <= output.getMaxStackSize()) {
                output.grow(rest.getCount());
                continue;
            }
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, rest);
        }
    }

    private void setActive(ServerLevel level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(ProcessingBlock.ACTIVE) && state.getValue(ProcessingBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ProcessingBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    private void inventoryChanged() {
        recipeDirty = true;
        setChanged();
    }

    /** Hook : appelé à chaque tick de progression (sons, particules). Par défaut ne fait rien. */
    protected void craftingTick(ServerLevel level, BlockPos pos) {
    }

    /** Hook : appelé quand une recette est terminée et sa sortie posée. Par défaut ne fait rien. */
    protected void craftFinished(ServerLevel level, BlockPos pos, ProcessingRecipe finished) {
    }

    /** Éjecte le contenu de l'inventaire dans le monde (appelé à la casse du bloc). */
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(slot));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag items = inventory.serializeNBT(registries);
        tag.put(TAG_INVENTORY, items);
        tag.putInt(TAG_PROGRESS, progress);
        tag.putInt(TAG_TOTAL, totalTime);
        if (lastUser != null) {
            tag.putUUID(TAG_LAST_USER, lastUser);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound(TAG_INVENTORY));
        progress = tag.getInt(TAG_PROGRESS);
        totalTime = tag.getInt(TAG_TOTAL);
        lastUser = tag.hasUUID(TAG_LAST_USER) ? tag.getUUID(TAG_LAST_USER) : null;
        recipeDirty = true;
    }
}
