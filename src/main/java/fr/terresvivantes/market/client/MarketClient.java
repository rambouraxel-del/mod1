package fr.terresvivantes.market.client;

import fr.terresvivantes.economy.logic.MarketLogic;
import fr.terresvivantes.market.ContractData;
import fr.terresvivantes.market.MarketModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.List;

/** Côté client : écrans de l'étal et du tableau, et calcul local de l'expiration pour les tooltips. Chargé seulement sur le client. */
public final class MarketClient {

    private MarketClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(MarketClient::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MarketModule.STALL_MENU.get(), StallScreen::new);
    }

    /** Ouvre le tableau avec la liste reçue du serveur. */
    public static void openBoard(List<ContractData> contracts) {
        Minecraft.getInstance().setScreen(new BoardScreen(contracts));
    }

    /** Vrai si le jour MC courant dépasse l'échéance (tooltip du contrat). */
    public static boolean isExpired(long deadlineDay) {
        Level level = Minecraft.getInstance().level;
        return level != null && MarketLogic.dayOf(level.getDayTime()) > deadlineDay;
    }
}
