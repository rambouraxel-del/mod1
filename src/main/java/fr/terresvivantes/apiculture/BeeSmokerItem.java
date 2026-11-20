package fr.terresvivantes.apiculture;

import fr.terresvivantes.apiculture.logic.ApiaryLogic;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Enfumoir (durabilité 64). Clic droit : fumée, et les abeilles vanilla de 8 blocs autour du joueur se calment.
 * Le joueur reste calme pendant {@link ApicultureConfig#calmSeconds()} secondes : la récolte ne les énerve pas.
 */
public final class BeeSmokerItem extends Item {

    /** Clé des données persistantes du joueur : heure de jeu jusqu'à laquelle la récolte est calme. */
    static final String CALM_UNTIL_KEY = "terresvivantes_smoked_until";

    public BeeSmokerItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            List<Bee> bees = server.getEntitiesOfClass(Bee.class,
                    player.getBoundingBox().inflate(ApiaryLogic.SMOKER_RADIUS));
            for (Bee bee : bees) {
                bee.setRemainingPersistentAngerTime(0);
                bee.setPersistentAngerTarget(null);
                bee.setTarget(null);
            }
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 1.0,
                    player.getZ(), 20, 0.4, 0.3, 0.4, 0.02);
            player.getPersistentData().putLong(CALM_UNTIL_KEY,
                    server.getGameTime() + ApicultureConfig.calmSeconds() * 20L);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Vrai si le joueur a utilisé l'enfumoir assez récemment pour une récolte calme. */
    public static boolean isCalm(Player player, long gameTime) {
        return player.getPersistentData().getLong(CALM_UNTIL_KEY) > gameTime;
    }
}
