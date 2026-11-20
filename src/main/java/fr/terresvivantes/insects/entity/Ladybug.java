package fr.terresvivantes.insects.entity;

import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.health.logic.Disease;
import fr.terresvivantes.insects.logic.AphidLogic;
import fr.terresvivantes.insects.logic.ButterflyLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Coccinelle (v0.3) : très petite, elle erre sur les cultures (préférence pour les blocs de culture) et saute
 * court. Ses élytres s'ouvrent en vol. Toutes les {@link AphidLogic#CURE_INTERVAL_TICKS} ticks d'IA, elle nettoie
 * avec {@link AphidLogic#CURE_CHANCE} une culture infestée proche. Elle réduit l'apparition des pucerons dans son
 * rayon (suppresseur enregistré par le module).
 */
public final class Ladybug extends PathfinderMob {

    /** Rayon horizontal (blocs) de recherche d'une culture infestée. */
    private static final int CURE_RADIUS = 2;
    /** Probabilité de sauter à un tick d'IA sur le sol. */
    private static final float HOP_CHANCE = 0.01f;

    private int aiTicks;

    public Ladybug(EntityType<? extends Ladybug> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    /** Préférence pour les cultures : une culture ou la terre labourée qui la porte attire la coccinelle. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getBlockState(pos).getBlock() instanceof CropBlock) {
            return 10.0f;
        }
        if (level.getBlockState(pos.below()).getBlock() instanceof FarmBlock) {
            return 4.0f;
        }
        return super.getWalkTargetValue(pos, level);
    }

    @Override
    protected void customServerAiStep() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.aiTicks++;
        if (this.aiTicks % AphidLogic.CURE_INTERVAL_TICKS == 0) {
            this.cureNearbyAphids(level);
        }
        if (this.onGround() && this.random.nextFloat() < HOP_CHANCE) {
            this.hop();
        }
    }

    /** Un saut court, vers une direction au hasard. */
    private void hop() {
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        this.setDeltaMovement(new Vec3(Math.cos(angle) * 0.25, 0.3, Math.sin(angle) * 0.25));
        this.hasImpulse = true;
    }

    /** Nettoie au plus une culture infestée proche, avec la chance de {@link AphidLogic#CURE_CHANCE}. */
    private void cureNearbyAphids(ServerLevel level) {
        BlockPos origin = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-CURE_RADIUS, -1, -CURE_RADIUS),
                origin.offset(CURE_RADIUS, 1, CURE_RADIUS))) {
            if (!level.isLoaded(pos) || HealthApi.diseaseOf(level, pos) != Disease.APHIDS) {
                continue;
            }
            if (AphidLogic.rollSucceeds(AphidLogic.CURE_CHANCE, this.random.nextDouble())) {
                HealthApi.cure(level, pos);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.0);
            }
            return;
        }
    }

    /**
     * Spawn naturel : sur le sol clair, au printemps ou en été, de jour. Rare: le poids est réglé par le biome modifier.
     */
    public static boolean checkSpawnRules(EntityType<Ladybug> type, LevelAccessor level, MobSpawnType spawnType,
                                          BlockPos pos, RandomSource random) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        boolean warm = ButterflyLogic.isWarmSeason(SeasonApi.getSeason(server));
        boolean onGrass = level.getBlockState(pos.below()).is(BlockTags.DIRT)
                || level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
        return warm && onGrass && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
    }
}
