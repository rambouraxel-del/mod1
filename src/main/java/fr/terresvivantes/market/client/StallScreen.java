package fr.terresvivantes.market.client;

import fr.terresvivantes.market.MarketStallMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Écran de l'étal : fond uni, cases encadrées, titres et inventaire gérés par la classe de base. */
public final class StallScreen extends AbstractContainerScreen<MarketStallMenu> {

    private static final int PANEL = 0xFFC6B38F;
    private static final int SLOT = 0xFF8B8B8B;

    public StallScreen(MarketStallMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 134;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, SLOT);
        }
    }
}
