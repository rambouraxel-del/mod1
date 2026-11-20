package fr.terresvivantes.livestock.item;

import fr.terresvivantes.livestock.LivestockApi;
import fr.terresvivantes.livestock.logic.Ailment;
import fr.terresvivantes.livestock.logic.Genome;
import fr.terresvivantes.livestock.logic.LivestockState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Registre d'élevage : clic droit sur un animal d'élevage pour lire ses gènes, sa nourriture, sa santé et son
 * affection (amour en cours ou non). Lecture seule : aucun état n'est modifié.
 */
public final class LivestockLedgerItem extends Item {

    public LivestockLedgerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Animal animal) || !LivestockApi.isLivestock(animal)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            report(serverPlayer, animal);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    /** Envoie le registre dans le chat du joueur (serveur uniquement). */
    private static void report(ServerPlayer player, Animal animal) {
        Genome genome = LivestockApi.getGenome(animal);
        LivestockState state = LivestockApi.getState(animal);
        player.sendSystemMessage(Component.translatable("message.terresvivantes.livestock.ledger.title",
                animal.getType().getDescription()));
        player.sendSystemMessage(Component.translatable("message.terresvivantes.livestock.ledger.genome",
                genome.productivity(), genome.size(), genome.hardiness(), genome.fertility()));
        player.sendSystemMessage(Component.translatable("message.terresvivantes.livestock.ledger.food",
                state.fed(), state.lastFedDay()));
        player.sendSystemMessage(Component.translatable("message.terresvivantes.livestock.ledger.health",
                state.health(), ailmentText(state.ailment())));
        player.sendSystemMessage(affectionText(animal));
    }

    private static Component ailmentText(Ailment ailment) {
        return switch (ailment) {
            case NONE -> Component.translatable("message.terresvivantes.livestock.ledger.ailment.none");
            case SICK -> Component.translatable("message.terresvivantes.livestock.ledger.ailment.sick");
            case PARASITES -> Component.translatable("message.terresvivantes.livestock.ledger.ailment.parasites");
        };
    }

    /** L'affection est l'état amoureux vanilla (non défini par le design v0.4) : amoureux, calme ou trop jeune. */
    private static Component affectionText(Animal animal) {
        if (animal.isInLove()) {
            return Component.translatable("message.terresvivantes.livestock.ledger.affection.in_love");
        }
        if (animal.isBaby()) {
            return Component.translatable("message.terresvivantes.livestock.ledger.affection.baby");
        }
        return Component.translatable("message.terresvivantes.livestock.ledger.affection.calm");
    }
}
