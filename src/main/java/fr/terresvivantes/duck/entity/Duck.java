package fr.terresvivantes.duck.entity;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.duck.DuckConfig;
import fr.terresvivantes.duck.DuckModule;
import fr.terresvivantes.duck.logic.DuckLogic;
import fr.terresvivantes.livestock.LivestockApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * Canard (v0.4, module duck) : animal d'élevage qui nage, flotte et se dandine. Il suit les graines (tag
 * {@link #FEED}), se reproduit, pond un œuf de canard à son rythme (multiplié par la productivité du génome) et
 * apparaît en apparence mâle (colvert) ou femelle (brune), choisie à la naissance et synchronisée avec le client.
 *
 * <p>La logique pure (délais, éclosion, battement) est dans {@link DuckLogic}. Le génome, la satiété et la
 * productivité viennent de la façade {@link LivestockApi}.
 */
public final class Duck extends Animal {

    /** Graines acceptées pour l'attirer et le nourrir : graines de blé et tag {@code c:seeds} (inclut les graines du mod). */
    public static final TagKey<Item> FEED = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "duck_feed"));

    private static final EntityDataAccessor<Boolean> IS_MALE =
            SynchedEntityData.defineId(Duck.class, EntityDataSerializers.BOOLEAN);

    private static final String TAG_EGG_TIME = "EggTime";
    private static final String TAG_IS_MALE = "IsMale";

    /** Ticks restant avant la prochaine ponte (décompté par la productivité). */
    private int eggTime;

    public Duck(EntityType<? extends Duck> type, Level level) {
        super(type, level);
        this.entityData.set(IS_MALE, this.random.nextBoolean());
        this.eggTime = DuckLogic.randomEggTime(this.random.nextDouble(), DuckConfig.eggMinTicks(), DuckConfig.eggMaxTicks());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    /**
     * Spawn naturel : règles d'un animal (herbe sous les pieds, lumière) et au moins une eau proche. Les biomes sont
     * filtrés par le biome modifier (rivières, marais, plaines).
     */
    public static boolean checkSpawnRules(EntityType<? extends Animal> type, LevelAccessor level, MobSpawnType spawnType,
                                          BlockPos pos, RandomSource random) {
        return Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random) && hasWaterNearby(level, pos);
    }

    private static boolean hasWaterNearby(LevelAccessor level, BlockPos pos) {
        int r = DuckLogic.WATER_SEARCH_RADIUS;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-r, -1, -r), pos.offset(r, 1, r))) {
            if (level.getFluidState(near).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_MALE, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterSeekGoal(this, 1.0));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Navigation qui autorise le passage sur l'eau : le canard nage et traverse les étangs. */
    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FEED);
    }

    @Override
    public Duck getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return DuckModule.DUCK.get().create(level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel) || this.isBaby()) {
            return;
        }
        float multiplier = LivestockApi.productionMultiplier(this);
        this.eggTime -= DuckLogic.roundProbabilistic(multiplier, this.random.nextDouble());
        if (this.eggTime <= 0) {
            layEgg();
        }
    }

    private void layEgg() {
        this.spawnAtLocation(DuckModule.DUCK_EGG.get());
        this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        this.eggTime = DuckLogic.randomEggTime(this.random.nextDouble(), DuckConfig.eggMinTicks(), DuckConfig.eggMaxTicks());
    }

    /** Vrai si le canard bat des ailes : en vol, en chute ou dans l'eau. */
    public boolean isWingFlapping() {
        return this.isInWater() || !this.onGround();
    }

    public boolean isMale() {
        return this.entityData.get(IS_MALE);
    }

    public void setMale(boolean male) {
        this.entityData.set(IS_MALE, male);
    }

    /** Ticks restant avant la ponte (lecture pour les tests et le débogage). */
    public int getEggTime() {
        return this.eggTime;
    }

    /** Fixe le délai de ponte (réservé aux tests de jeu). */
    public void setEggTime(int ticks) {
        this.eggTime = ticks;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_EGG_TIME, this.eggTime);
        tag.putBoolean(TAG_IS_MALE, isMale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_EGG_TIME)) {
            this.eggTime = tag.getInt(TAG_EGG_TIME);
        }
        if (tag.contains(TAG_IS_MALE)) {
            setMale(tag.getBoolean(TAG_IS_MALE));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHICKEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.CHICKEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHICKEN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    /** Voix plus grave que le poulet : le cri du canard. */
    @Override
    public float getVoicePitch() {
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.8F;
    }
}
