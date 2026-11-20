package fr.terresvivantes.apiculture;

import fr.terresvivantes.apiculture.logic.ApiaryLogic;
import fr.terresvivantes.apiculture.logic.HoneyType;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * BlockEntity de l'apiaire : population de la colonie, quatre cadres et leur progression.
 * Le serveur appelle {@link #serverTick} à chaque tick; toutes les {@link ApiaryLogic#CYCLE_TICKS} ticks,
 * un cycle fait évoluer la colonie et les cadres.
 */
public final class ApiaryBlockEntity extends BlockEntity {

    private final ApiaryInventory inventory = new ApiaryInventory(this);
    private final float[] progress = new float[ApiaryLogic.FRAME_SLOTS];
    private int population;
    private int cycleTicks;
    /** Heure de jeu du peuplement depuis une ruche voisine, ou -1 s'il n'est pas programmé. */
    private long settleAt = -1L;

    public ApiaryBlockEntity(BlockPos pos, BlockState state) {
        super(ApicultureModule.APIARY_BE.get(), pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null && !level.isClientSide) {
            ApicultureApi.track(this);
        }
    }

    @Override
    public void setRemoved() {
        ApicultureApi.untrack(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        ApicultureApi.untrack(this);
        super.onChunkUnloaded();
    }

    /** Appelé par le ticker serveur à chaque tick. Ne fait rien avant la fin de la période. */
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (++cycleTicks < ApiaryLogic.CYCLE_TICKS) {
            return;
        }
        cycleTicks = 0;
        if (settleAt >= 0 && level.getGameTime() >= settleAt) {
            settleAt = -1L;
            if (population == 0 && hasPopulatedHiveNearby(level, pos)) {
                population = ApiaryLogic.SETTLED_POPULATION;
            }
        }
        cycle(level, ApiaryLogic.seasonFactor(SeasonApi.getSeason(level)));
    }

    /**
     * Un cycle de 200 ticks : absorption d'une abeille si l'apiaire est vide, évolution de la population,
     * progression des cadres vides (miel dominant du rayon). {@code season} vient de
     * {@link ApiaryLogic#seasonFactor}; 0 = dormance. Public pour les tests de jeu.
     */
    public void cycle(ServerLevel level, float season) {
        BlockPos pos = getBlockPos();
        if (population == 0 && season > 0f) {
            List<Bee> bees = level.getEntitiesOfClass(Bee.class, new AABB(pos).inflate(ApiaryLogic.ABSORB_RADIUS));
            if (ApiaryLogic.shouldAbsorb(population, season, bees.size(),
                    level.getRandom().nextDouble(), ApicultureConfig.absorbChance())) {
                bees.get(0).discard();
                population = ApiaryLogic.ABSORBED_POPULATION;
            }
        }

        int[] flora = scanFlora(level, pos);
        int flowers = ApiaryLogic.flowerTotal(flora);
        population = ApiaryLogic.nextPopulation(population, flowers, season, level.getRandom().nextDouble());

        HoneyType dominant = ApiaryLogic.dominantHoney(flora);
        if (dominant != null && population > 0) {
            float increment = ApiaryLogic.productionIncrement(population, flowers, season);
            for (int slot = 0; slot < ApiaryLogic.FRAME_SLOTS; slot++) {
                if (!inventory.getStackInSlot(slot).is(ApicultureModule.EMPTY_FRAME.get())) {
                    progress[slot] = 0f;
                    continue;
                }
                progress[slot] = ApiaryLogic.advance(progress[slot], increment);
                if (ApiaryLogic.isComplete(progress[slot])) {
                    progress[slot] = 0f;
                    inventory.setStackInSlot(slot, honeyFrame(dominant));
                }
            }
        }
        syncBlock();
    }

    /** Appelé au placement : si une ruche vanilla habitée est proche, l'apiaire se peuple après un jour. */
    public void onPlaced(ServerLevel level, BlockPos pos) {
        if (hasPopulatedHiveNearby(level, pos)) {
            settleAt = level.getGameTime() + ApiaryLogic.SETTLE_DELAY_TICKS;
            setChanged();
        }
    }

    /** Insère un cadre vide dans le premier emplacement libre. Vrai si le cadre est entré. */
    public boolean insertEmptyFrame() {
        for (int slot = 0; slot < ApiaryLogic.FRAME_SLOTS; slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                inventory.setStackInSlot(slot, new ItemStack(ApicultureModule.EMPTY_FRAME.get()));
                progress[slot] = 0f;
                return true;
            }
        }
        return false;
    }

    /**
     * Récolte les cadres de miel dans l'inventaire du joueur. Sans calme (enfumoir en main d'en face ou fumée
     * récente), une colonie de plus de {@link ApicultureConfig#angryThreshold()} abeilles attaque le joueur.
     * Renvoie le nombre de cadres récoltés.
     */
    public int harvest(ServerLevel level, Player player) {
        int harvested = 0;
        for (int slot = 0; slot < ApiaryLogic.FRAME_SLOTS; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.is(ApicultureModule.HONEY_FRAME.get())) {
                ItemStack taken = stack.copy();
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(taken);
                harvested++;
            }
        }
        if (harvested > 0) {
            boolean calm = BeeSmokerItem.isCalm(player, level.getGameTime())
                    || player.getOffhandItem().is(ApicultureModule.BEE_SMOKER.get());
            if (ApiaryLogic.angersBees(population, ApicultureConfig.angryThreshold(), calm)) {
                spawnAngryBees(level, getBlockPos(), player);
            }
        }
        return harvested;
    }

    /** Texte d'état pour l'action bar : population, cadres et miel dominant. */
    public Component describe() {
        Level level = getLevel();
        HoneyType dominant = level == null ? null : ApiaryLogic.dominantHoney(scanFlora(level, getBlockPos()));
        Component honey = dominant == null
                ? Component.translatable("message.terresvivantes.apiary.no_honey")
                : Component.translatable(ApicultureModule.honeyItem(dominant).getDescriptionId());
        return Component.translatable("message.terresvivantes.apiary.status",
                population, ApiaryLogic.MAX_POPULATION, frameCount(), ApiaryLogic.FRAME_SLOTS, honey);
    }

    /** Drops des cadres à la destruction du bloc. */
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < ApiaryLogic.FRAME_SLOTS; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
    }

    public boolean isActive() {
        return population > 0;
    }

    public int population() {
        return population;
    }

    /** Fixe la population (bornée à [0, 100]). Usage : tests et administration. */
    public void setPopulation(int value) {
        population = Mth.clamp(value, 0, ApiaryLogic.MAX_POPULATION);
        syncBlock();
    }

    /** Copie du cadre de l'emplacement donné (vide si aucun). */
    public ItemStack frame(int slot) {
        return inventory.getStackInSlot(slot).copy();
    }

    /** Inventaire exposé aux trémies (capacité ItemHandler). */
    ApiaryInventory inventory() {
        return inventory;
    }

    /** Appelé par l'inventaire quand un emplacement change. */
    void onInventoryChanged() {
        syncBlock();
    }

    private int frameCount() {
        int count = 0;
        for (int slot = 0; slot < ApiaryLogic.FRAME_SLOTS; slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static ItemStack honeyFrame(HoneyType type) {
        ItemStack stack = new ItemStack(ApicultureModule.HONEY_FRAME.get());
        stack.set(ApicultureModule.HONEY_TYPE.get(), type);
        return stack;
    }

    /** Compte les fleurs par type de miel dans le rayon horizontal, à 2 blocs près en hauteur. */
    static int[] scanFlora(Level level, BlockPos center) {
        int[] counts = new int[HoneyType.values().length];
        int radius = ApiaryLogic.FLOWER_RADIUS;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    HoneyType type = ApicultureApi.nectarOf(level.getBlockState(cursor));
                    if (type != null) {
                        counts[type.ordinal()]++;
                    }
                }
            }
        }
        return counts;
    }

    /** Ruche ou nid vanilla contenant au moins une abeille, dans le cube de rayon {@link ApiaryLogic#SETTLE_RADIUS}. */
    private static boolean hasPopulatedHiveNearby(Level level, BlockPos center) {
        int radius = ApiaryLogic.SETTLE_RADIUS;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive
                    && !hive.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void spawnAngryBees(ServerLevel level, BlockPos pos, LivingEntity target) {
        for (int i = 0; i < ApiaryLogic.ANGRY_BEES; i++) {
            Bee bee = new Bee(EntityType.BEE, level);
            bee.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    level.getRandom().nextFloat() * 360f, 0f);
            bee.setRemainingPersistentAngerTime(ApiaryLogic.ANGER_TICKS);
            bee.setPersistentAngerTarget(target.getUUID());
            bee.setTarget(target);
            level.addFreshEntity(bee);
        }
    }

    /** Met à jour l'état HONEY du bloc et prévient les clients. */
    private void syncBlock() {
        setChanged();
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean honey = frameContainsHoney();
        if (state.getValue(ApiaryBlock.HONEY) != honey) {
            state = state.setValue(ApiaryBlock.HONEY, honey);
            level.setBlock(getBlockPos(), state, Block.UPDATE_ALL);
        }
        level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }

    private boolean frameContainsHoney() {
        for (int slot = 0; slot < ApiaryLogic.FRAME_SLOTS; slot++) {
            if (inventory.getStackInSlot(slot).is(ApicultureModule.HONEY_FRAME.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("population", population);
        tag.putLong("settle", settleAt);
        tag.put("frames", inventory.serializeNBT(registries));
        for (int slot = 0; slot < ApiaryLogic.FRAME_SLOTS; slot++) {
            tag.putFloat("progress" + slot, progress[slot]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        population = Mth.clamp(tag.getInt("population"), 0, ApiaryLogic.MAX_POPULATION);
        settleAt = tag.contains("settle") ? tag.getLong("settle") : -1L;
        inventory.deserializeNBT(registries, tag.getCompound("frames"));
        for (int slot = 0; slot < ApiaryLogic.FRAME_SLOTS; slot++) {
            progress[slot] = tag.getFloat("progress" + slot);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
