package fr.terresvivantes.guide;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Remise du manuel à la première connexion, une seule fois par joueur. */
public final class GuideEvents {

    private GuideEvents() {
    }

    static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            grantFirstManual(event.getEntity());
        }
    }

    /**
     * Remet le manuel si le joueur ne l'a jamais reçu et si la config l'autorise.
     * Le drapeau {@code manual_given} est posé dès la remise; il est conservé à la mort.
     *
     * @return vrai si l'item a été remis par cet appel
     */
    public static boolean grantFirstManual(Player player) {
        if (!GuideConfig.giveOnFirstJoin() || player.getData(GuideModule.MANUAL_GIVEN.get())) {
            return false;
        }
        player.setData(GuideModule.MANUAL_GIVEN.get(), true);
        ItemStack manual = new ItemStack(GuideModule.FARMERS_MANUAL.get());
        if (!player.getInventory().add(manual)) {
            player.drop(manual, false);
        }
        return true;
    }
}
