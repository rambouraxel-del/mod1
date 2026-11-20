package fr.terresvivantes.processing.client;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.processing.ProcessingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Écran générique des machines : fond {@code textures/gui/processing.png} (176 x 166) et barre de progression
 * dessinée par-dessus la flèche (couleur pleine, 24 pixels au maximum).
 */
public final class ProcessingScreen extends AbstractContainerScreen<ProcessingMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TerresVivantes.MODID, "textures/gui/processing.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;
    private static final int PROGRESS_COLOR = 0xFFE0A030;

    public ProcessingScreen(ProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        int width = menu.progressScaled(ProcessingMenu.ARROW_WIDTH);
        if (width > 0) {
            graphics.fill(x + 79, y + 40, x + 79 + width, y + 46, PROGRESS_COLOR);
        }
    }
}
