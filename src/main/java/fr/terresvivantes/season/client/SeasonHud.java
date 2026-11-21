package fr.terresvivantes.season.client;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.season.SeasonConfig;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * HUD saison, haut a gauche: pastille de couleur de la saison et "Printemps · Jour 3/7".
 * Classe cote client uniquement: n'est enregistree que si le dist est CLIENT (voir SeasonModule).
 */
public final class SeasonHud {

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "season_hud");
    private static final int MARGIN = 6;
    private static final int SWATCH = 8;
    private static final int TEXT_GAP = 4;

    private SeasonHud() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SeasonHud::onRegisterGuiLayers);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, SeasonHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!SeasonConfig.hudEnabled() || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        Season season = ClientSeasonState.season();
        Component text = Component.translatable("terresvivantes.hud.season",
                Component.translatable(season.langKey()),
                ClientSeasonState.dayOfSeason(),
                ClientSeasonState.daysPerSeason());
        graphics.fill(MARGIN, MARGIN, MARGIN + SWATCH, MARGIN + SWATCH, 0xFF000000 | color(season));
        graphics.drawString(minecraft.font, text, MARGIN + SWATCH + TEXT_GAP, MARGIN, 0xFFFFFF, true);
    }

    private static int color(Season season) {
        return switch (season) {
            case SPRING -> 0x7CB342;
            case SUMMER -> 0xFBC02D;
            case AUTUMN -> 0xD9822B;
            case WINTER -> 0x81D4FA;
        };
    }
}
