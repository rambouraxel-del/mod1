package fr.terresvivantes.market.client;

import fr.terresvivantes.core.quality.logic.QualityLogic;
import fr.terresvivantes.market.ContractData;
import fr.terresvivantes.market.ContractItem;
import fr.terresvivantes.market.MarketNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Tableau d'annonces : une ligne par contrat ouvert (produit, quantité, qualité, échéance, gain) et le bouton Accepter. */
public final class BoardScreen extends Screen {

    private static final int ROW_Y = 40;
    private static final int ROW_HEIGHT = 30;
    private static final int PANEL_WIDTH = 300;

    private final List<ContractData> contracts;

    public BoardScreen(List<ContractData> contracts) {
        super(Component.translatable("screen.terresvivantes.market_board.title"));
        this.contracts = contracts;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - PANEL_WIDTH / 2;
        for (int row = 0; row < contracts.size(); row++) {
            ContractData contract = contracts.get(row);
            int y = ROW_Y + row * ROW_HEIGHT;
            addRenderableWidget(Button.builder(Component.translatable("screen.terresvivantes.market_board.accept"),
                            button -> accept(contract.id()))
                    .bounds(left + PANEL_WIDTH - 64, y + 4, 60, 20)
                    .build());
        }
    }

    private void accept(long id) {
        PacketDistributor.sendToServer(new MarketNetwork.AcceptContractPayload(id));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, this.width / 2, 14, 0xFFFFFF);
        int left = this.width / 2 - PANEL_WIDTH / 2;
        if (contracts.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.terresvivantes.market_board.empty"),
                    this.width / 2, ROW_Y, 0xAAAAAA);
            return;
        }
        for (int row = 0; row < contracts.size(); row++) {
            ContractData contract = contracts.get(row);
            int y = ROW_Y + row * ROW_HEIGHT;
            Item product = ContractItem.itemOf(contract.product());
            graphics.renderItem(new ItemStack(product), left + 4, y + 4);
            graphics.drawString(font, Component.translatable("screen.terresvivantes.market_board.line",
                            Component.translatable(product.getDescriptionId()), contract.quantity(),
                            QualityLogic.stars(contract.minQuality()), contract.deadlineDay()),
                    left + 26, y + 3, 0xFFFFFF, false);
            graphics.drawString(font, Component.translatable("screen.terresvivantes.market_board.reward",
                            contract.reward(), contract.reputation()),
                    left + 26, y + 14, 0xFFD27F, false);
        }
    }
}
