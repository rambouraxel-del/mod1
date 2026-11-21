package fr.terresvivantes.processing.client;

import fr.terresvivantes.processing.ProcessingModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Côté client : associe le menu générique à son écran. Chargé seulement sur le client. */
public final class ProcessingClient {

    private ProcessingClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ProcessingClient::onRegisterScreens);
    }

    private static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ProcessingModule.MENU.get(), ProcessingScreen::new);
    }
}
