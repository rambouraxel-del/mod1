package fr.terresvivantes.pests.entity;

import fr.terresvivantes.pests.PestsConfig;
import fr.terresvivantes.pests.logic.CrowLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

/**
 * Corbeau (module pests) : oiseau volant et neutre. Il s'enfuit d'un joueur à moins de
 * {@link CrowLogic#FLEE_DISTANCE} blocs, fuit les épouvantails et se pose sur les cultures mûres pour picorer
 * (voir {@link PeckCropGoal}). La logique pure est dans {@link CrowLogic}.
 */
public final class Crow extends PathfinderMob {

    /** Vrai pendant que le corbeau est posé sur une culture (synchronisé pour l'animation). */
    private static final EntityDataAccessor<Boolean> PECKING =
            SynchedEntityData.defineId(Crow.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private BlockPos peckTarget;

    public Crow(EntityType<? extends Crow> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6);
    }

    /**
     * Spawn naturel : règles d'un mob de surface, désactivable par config, et au plus
     * {@link CrowLogic#MAX_CROWS_PER_AREA} corbeaux dans un cube de {@link CrowLogic#CROW_AREA_SIZE} blocs.
     */
    public static boolean checkSpawnRules(EntityType<? extends Mob> type, LevelAccessor level, MobSpawnType spawnType,
                                          BlockPos pos, RandomSource random) {
        if (!PestsConfig.crowsEnabled()) {
            return false;
        }
        if (level instanceof ServerLevel server) {
            int half = CrowLogic.CROW_AREA_SIZE / 2;
            int nearby = server.getEntitiesOfClass(Crow.class, new AABB(pos).inflate(half)).size();
            if (!CrowLogic.spawnAllowed(nearby)) {
                return false;
            }
        }
        return Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PECKING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new AvoidEntityGoal<>(this, Player.class, (float) CrowLogic.FLEE_DISTANCE, 1.2D, 1.5D));
        this.goalSelector.addGoal(1, new RepelFromScarecrowGoal(this));
        this.goalSelector.addGoal(2, new PeckCropGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    /** Posé sur une culture : le corbeau cesse de voler et reste sur la terre, sous la culture. */
    public boolean isPecking() {
        return this.entityData.get(PECKING);
    }

    @Nullable
    public BlockPos peckTarget() {
        return peckTarget;
    }

    /** Commence à se poser sur la culture en {@code crop}. */
    public void startPecking(BlockPos crop) {
        this.peckTarget = crop.immutable();
        this.setNoGravity(false);
        this.entityData.set(PECKING, true);
    }

    /** Quitte la culture et reprend son vol. */
    public void stopPecking() {
        this.peckTarget = null;
        this.setNoGravity(true);
        this.entityData.set(PECKING, false);
    }

    /** Maintient le corbeau au niveau de la culture, à l'arrêt. */
    public void perchOn(BlockPos crop) {
        this.setPos(crop.getX() + 0.5, crop.getY(), crop.getZ() + 0.5);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        this.getNavigation().stop();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    /** Voix grave : croassement plutôt que cri de perroquet. */
    @Override
    public float getVoicePitch() {
        return 0.5F + this.random.nextFloat() * 0.1F;
    }
}
