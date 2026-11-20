package fr.terresvivantes.market;

import fr.terresvivantes.economy.EconomyApi;
import fr.terresvivantes.economy.EconomyModule;
import fr.terresvivantes.market.logic.StallLogic;
import fr.terresvivantes.market.logic.TillLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Étal (v0.7) : 9 cases de vente et une case caisse. Toutes les {@link StallLogic#SALE_INTERVAL_TICKS} ticks, un acheteur
 * prend un item (le mieux payé) s'il y a un villageois à portée ou si c'est le jour. Les pièces vont dans la caisse,
 * qui fusionne automatiquement cuivre, argent et or ({@link TillLogic}).
 */
public final class MarketStallBlockEntity extends BlockEntity implements Container {

    public static final int SALE_SLOTS = 9;
    public static final int TILL_SLOT = 9;
    private static final int SIZE = 10;
    private static final String TAG_TILL = "Till";

    private final NonNullList<ItemStack> sale = NonNullList.withSize(SALE_SLOTS, ItemStack.EMPTY);
    /** Solde de la caisse en cuivre : la case n'en montre que la pièce la plus grosse qui la représente. */
    private long till;
    private int ticks;

    public MarketStallBlockEntity(BlockPos pos, BlockState state) {
        super(MarketModule.STALL_BE.get(), pos, state);
    }

    /** Vrai pour les pièces de cuivre, d'argent et d'or. */
    public static boolean isCoin(ItemStack stack) {
        return denominationOf(stack.getItem()) >= 0;
    }

    private static int denominationOf(Item item) {
        if (item == EconomyModule.COPPER_COIN.get()) {
            return TillLogic.COPPER;
        }
        if (item == EconomyModule.SILVER_COIN.get()) {
            return TillLogic.SILVER;
        }
        if (item == EconomyModule.GOLD_COIN.get()) {
            return TillLogic.GOLD;
        }
        return -1;
    }

    /** Solde de la caisse en cuivre. */
    public long tillValue() {
        return till;
    }

    /** Appelé à chaque tick serveur : déclenche un cycle de vente toutes les 2400 ticks. */
    public void serverTick(ServerLevel level) {
        ticks++;
        if (ticks >= StallLogic.SALE_INTERVAL_TICKS) {
            ticks = 0;
            runSaleCycle(level);
        }
    }

    /** Un cycle de vente : un item vendu si un acheteur est présent (villageois à portée ou jour). */
    public void runSaleCycle(ServerLevel level) {
        BlockPos pos = getBlockPos();
        Villager villager = nearestVillager(level, pos);
        boolean day = StallLogic.isDay(level.getDayTime());
        if (!StallLogic.shouldSell(villager != null, day)) {
            return;
        }
        int[] prices = new int[SALE_SLOTS];
        for (int slot = 0; slot < SALE_SLOTS; slot++) {
            ItemStack stack = sale.get(slot);
            prices[slot] = stack.isEmpty() ? 0 : EconomyApi.priceOf(level, stack);
        }
        int chosen = StallLogic.pickIndex(prices);
        if (chosen < 0) {
            return;
        }
        ItemStack stack = sale.get(chosen);
        EconomyApi.recordSale(level, stack);
        till += StallLogic.payment(prices[chosen]);
        stack.shrink(1);
        if (stack.isEmpty()) {
            sale.set(chosen, ItemStack.EMPTY);
        }
        setChanged();
        if (villager != null) {
            Vec3 at = villager.position();
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + villager.getBbHeight(), at.z, 6,
                    0.3, 0.4, 0.3, 0.0);
        }
        level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Nullable
    private static Villager nearestVillager(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double range = StallLogic.VILLAGER_RANGE;
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(range),
                v -> v.isAlive() && v.distanceToSqr(center) <= range * range);
        return villagers.stream().min(Comparator.comparingDouble(v -> v.distanceToSqr(center))).orElse(null);
    }

    /** Case caisse : la pile montrée pour le solde courant. */
    private ItemStack tillStack() {
        int[] shown = TillLogic.displayFor(till);
        if (shown[0] < 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(coinItem(shown[0]), shown[1]);
    }

    private static Item coinItem(int denomination) {
        return switch (denomination) {
            case TillLogic.SILVER -> EconomyModule.SILVER_COIN.get();
            case TillLogic.GOLD -> EconomyModule.GOLD_COIN.get();
            default -> EconomyModule.COPPER_COIN.get();
        };
    }

    /** Lâche le contenu au sol (cases et solde en pièces); appelé quand l'étal est retiré. */
    public void dropContents(ServerLevel level, BlockPos pos) {
        for (ItemStack stack : sale) {
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5,
                        pos.getZ() + 0.5, stack);
            }
        }
        for (ItemStack coins : EconomyApi.copperToCoins((int) Math.min(Integer.MAX_VALUE, till))) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5,
                    pos.getZ() + 0.5, coins);
        }
        emptySale();
        till = 0;
    }

    private void emptySale() {
        for (int i = 0; i < SALE_SLOTS; i++) {
            sale.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, sale, registries);
        tag.putLong(TAG_TILL, till);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, sale, registries);
        till = Math.max(0L, tag.getLong(TAG_TILL));
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return till <= 0 && sale.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == TILL_SLOT) {
            return tillStack();
        }
        return slot >= 0 && slot < SALE_SLOTS ? sale.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == TILL_SLOT) {
            return takeFromTill(amount);
        }
        ItemStack taken = ContainerHelper.removeItem(sale, slot, amount);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == TILL_SLOT) {
            return takeFromTill(Integer.MAX_VALUE);
        }
        return slot >= 0 && slot < SALE_SLOTS ? ContainerHelper.takeItem(sale, slot) : ItemStack.EMPTY;
    }

    /** Retire des pièces de la caisse : le solde baisse de leur valeur. */
    private ItemStack takeFromTill(int amount) {
        ItemStack shown = tillStack();
        if (shown.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int count = Math.min(amount, shown.getCount());
        till = Math.max(0L, till - TillLogic.valueOf(denominationOf(shown.getItem()), count));
        setChanged();
        return new ItemStack(shown.getItem(), count);
    }

    /** Pose une case : pour la caisse, le solde est recalculé d'après la valeur des pièces posées. */
    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == TILL_SLOT) {
            long before = TillLogic.valueOf(denominationOf(tillStack().getItem()), tillStack().getCount());
            long after = isCoin(stack) ? TillLogic.valueOf(denominationOf(stack.getItem()), stack.getCount()) : 0L;
            till = Math.max(0L, till - before + after);
        } else if (slot >= 0 && slot < SALE_SLOTS) {
            sale.set(slot, stack);
            if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return TillLogic.MAX_STACK;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        emptySale();
        till = 0;
    }
}
