package fr.terresvivantes.insects.entity;

import fr.terresvivantes.insects.logic.ButterflyLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Papillon (v0.3) : insecte ambiant qui vole doucement, se pose sur les fleurs (tag {@code minecraft:flowers}) et
 * se cache la nuit, en hiver et sous la pluie. Quatre variantes couleur, synchronisées avec le client.
 *
 * <p>Le mouvement est propre à l'entité (pas de navigation) : un point cible, vitesse faible, et un repos de quelques
 * secondes sur une fleur. Les règles de saison et d'activité sont dans {@link ButterflyLogic}.
 */
public final class Butterfly extends PathfinderMob {

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(Butterfly.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RESTING =
            SynchedEntityData.defineId(Butterfly.class, EntityDataSerializers.BOOLEAN);

    /** Rayon de recherche d'une fleur, en blocs horizontaux. */
    private static final int FLOWER_RADIUS = 8;
    /** Hauteur de recherche d'une fleur, en blocs verticaux. */
    private static final int FLOWER_HEIGHT = 2;
    /** Vitesse de vol, en blocs par tick. */
    private static final double SPEED = 0.05;
    /** Distance à laquelle le papillon considère sa cible atteinte. */
    private static final double REACH = 0.5;
    /** Durée maximale d'un trajet vers une cible, en ticks. */
    private static final int TRIP_TICKS = 200;
    /** Repos sur une fleur, en ticks (minimum et étendue aléatoire). */
    private static final int REST_MIN = 60;
    private static final int REST_SPREAD = 120;

    @Nullable
    private Vec3 goal;
    private boolean goalIsFlower;
    private int tripTicks;
    private int restUntil;
    private int inactiveTicks;

    public Butterfly(EntityType<? extends Butterfly> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.MOVEMENT_SPEED, SPEED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(RESTING, false);
    }

    public int getVariant() {
        return ButterflyLogic.clampVariant(this.entityData.get(VARIANT));
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, ButterflyLogic.clampVariant(variant));
    }

    /** Vrai pendant le repos sur une fleur (ailes presque fermées). Synchronisé avec le client. */
    public boolean isResting() {
        return this.entityData.get(RESTING);
    }

    private void setResting(boolean resting) {
        this.entityData.set(RESTING, resting);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        this.setVariant(this.random.nextInt(ButterflyLogic.VARIANT_COUNT));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    @Override
    protected void customServerAiStep() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        boolean active = ButterflyLogic.isActive(SeasonApi.getSeason(level),
                ButterflyLogic.isDaytime(level.getDayTime()), level.isRaining());
        if (!active) {
            this.hideOrWait();
            return;
        }
        this.inactiveTicks = 0;
        if (this.isResting()) {
            this.restOnFlower();
            return;
        }
        if (this.goal == null || this.tripTicks-- <= 0) {
            this.chooseGoal(level);
        }
        if (this.goal == null) {
            return;
        }
        Vec3 offset = this.goal.subtract(this.position());
        if (offset.lengthSqr() < REACH * REACH) {
            this.arrive(level);
            return;
        }
        Vec3 wanted = offset.normalize().scale(SPEED);
        this.setDeltaMovement(this.getDeltaMovement().lerp(wanted, 0.2));
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setYRot(-(float) Math.toDegrees(Math.atan2(offset.x, offset.z)));
    }

    /** Inactif (nuit, hiver, pluie) : il reste en place, puis se retire après un délai. */
    private void hideOrWait() {
        this.goal = null;
        this.setResting(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.inactiveTicks++;
        if (this.inactiveTicks > ButterflyLogic.HIDE_TICKS) {
            this.discard();
        }
    }

    private void restOnFlower() {
        this.setDeltaMovement(Vec3.ZERO);
        if (this.tickCount >= this.restUntil) {
            this.setResting(false);
            this.goal = null;
        }
    }

    private void arrive(ServerLevel level) {
        if (this.goalIsFlower) {
            this.setResting(true);
            this.restUntil = this.tickCount + REST_MIN + this.random.nextInt(REST_SPREAD);
            this.setDeltaMovement(Vec3.ZERO);
            this.goal = null;
            this.goalIsFlower = false;
        } else {
            this.chooseGoal(level);
        }
    }

    /** Choisit une fleur voisine si possible, sinon un point de vol au hasard près de la position courante. */
    private void chooseGoal(ServerLevel level) {
        this.tripTicks = TRIP_TICKS;
        BlockPos flower = this.findFlower(level);
        if (flower != null) {
            this.goal = Vec3.atBottomCenterOf(flower).add(0, 0.4, 0);
            this.goalIsFlower = true;
            return;
        }
        double dx = (this.random.nextDouble() - 0.5) * 6.0;
        double dz = (this.random.nextDouble() - 0.5) * 6.0;
        double dy = (this.random.nextDouble() - 0.5) * 2.0;
        this.goal = this.position().add(dx, dy, dz);
        this.goalIsFlower = false;
    }

    @Nullable
    private BlockPos findFlower(ServerLevel level) {
        List<BlockPos> flowers = new ArrayList<>();
        BlockPos origin = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-FLOWER_RADIUS, -FLOWER_HEIGHT, -FLOWER_RADIUS),
                origin.offset(FLOWER_RADIUS, FLOWER_HEIGHT, FLOWER_RADIUS))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(BlockTags.FLOWERS)) {
                flowers.add(pos.immutable());
            }
        }
        if (flowers.isEmpty()) {
            return null;
        }
        return flowers.get(this.random.nextInt(flowers.size()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
    }

    /**
     * Spawn naturel : jour, printemps ou été, hors pluie, posé sur une herbe, des feuilles ou des fleurs.
     */
    public static boolean checkSpawnRules(EntityType<Butterfly> type, LevelAccessor level, MobSpawnType spawnType,
                                          BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        boolean habitat = below.is(BlockTags.LEAVES) || below.is(BlockTags.FLOWERS)
                || below.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        return habitat && ButterflyLogic.isActive(SeasonApi.getSeason(server),
                ButterflyLogic.isDaytime(server.getDayTime()), server.isRaining());
    }
}
