package fr.terresvivantes.duck.entity;

import fr.terresvivantes.duck.DuckModule;
import fr.terresvivantes.duck.logic.DuckLogic;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * Œuf de canard lancé. Comme l'œuf de poule, il éclôt en caneton avec une chance de 1 sur 8 au premier impact.
 */
public final class DuckEggEntity extends ThrowableItemProjectile {

    public DuckEggEntity(EntityType<? extends DuckEggEntity> type, Level level) {
        super(type, level);
    }

    public DuckEggEntity(EntityType<? extends DuckEggEntity> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return DuckModule.DUCK_EGG.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        if (DuckLogic.hatchesFromThrow(random.nextDouble())) {
            Duck duckling = DuckModule.DUCK.get().create(level());
            if (duckling != null) {
                duckling.setBaby(true);
                duckling.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
                level().addFreshEntity(duckling);
            }
        }
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }
}
