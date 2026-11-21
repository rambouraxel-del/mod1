package fr.terresvivantes.guide.client;

import fr.terresvivantes.guide.logic.GuideBook;
import fr.terresvivantes.guide.logic.GuideChapter;
import fr.terresvivantes.guide.logic.GuidePage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran du manuel : fond de livre vanilla ({@code textures/gui/book.png}), une étape à la fois.
 * Étape 0 : table des matières cliquable. Étapes suivantes : titre, icône d'item optionnelle et texte.
 * Flèches gauche et droite du clavier pour tourner les pages.
 */
public final class GuideScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;
    private static final int TEXT_X = 22;
    private static final int TEXT_WIDTH = 148;
    private static final int LINE_HEIGHT = 10;
    private static final int INK = 0xFF2B1B0E;
    private static final int MUTED = 0xFF6B5A44;
    private static final int BOTTOM_LIMIT = BOOK_HEIGHT - 28;

    private final GuideBook book;
    private final List<Button> chapterButtons = new ArrayList<>();
    private int step;
    private int left;
    private int top;
    private Button previous;
    private Button next;
    private Button contents;

    public GuideScreen(GuideBook book) {
        super(Component.translatable("item.terresvivantes.farmers_manual"));
        this.book = book;
    }

    @Override
    protected void init() {
        left = (width - BOOK_WIDTH) / 2;
        top = (height - BOOK_HEIGHT) / 2;
        chapterButtons.clear();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1))
                .bounds(left + 12, top + BOOK_HEIGHT - 22, 24, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
                .bounds(left + BOOK_WIDTH - 36, top + BOOK_HEIGHT - 22, 24, 16).build());
        contents = addRenderableWidget(Button.builder(Component.translatable("terresvivantes.guide.toc"), b -> goTo(0))
                .bounds(left + BOOK_WIDTH / 2 - 32, top + BOOK_HEIGHT - 22, 64, 16).build());
        for (int i = 0; i < book.chapters().size(); i++) {
            int chapter = i;
            chapterButtons.add(addRenderableWidget(Button.builder(
                            Component.literal(book.chapters().get(i).title()),
                            b -> goTo(book.firstStepOf(chapter)))
                    .bounds(left + 16, top + 34 + i * 12, 160, 12).build()));
        }
        updateControls();
    }

    private void turn(int delta) {
        goTo(step + delta);
    }

    private void goTo(int target) {
        step = Math.max(0, Math.min(target, book.stepCount() - 1));
        updateControls();
    }

    private void updateControls() {
        boolean cover = step == 0;
        previous.active = step > 0;
        next.active = step < book.stepCount() - 1;
        contents.active = !cover;
        for (Button button : chapterButtons) {
            button.visible = cover;
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(BOOK_TEXTURE, left, top, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);
        if (step == 0) {
            drawCover(graphics);
        } else {
            drawPage(graphics);
        }
    }

    private void drawCover(GuiGraphics graphics) {
        graphics.drawCenteredString(font, title, left + BOOK_WIDTH / 2, top + 14, INK);
        graphics.drawCenteredString(font, Component.translatable("terresvivantes.guide.toc"),
                left + BOOK_WIDTH / 2, top + 24, MUTED);
    }

    private void drawPage(GuiGraphics graphics) {
        int chapterIndex = book.chapterOfStep(step);
        GuideChapter chapter = book.chapters().get(chapterIndex);
        GuidePage page = book.pageAt(step);

        String counter = step + " / " + book.pageCount();
        graphics.drawString(font, chapter.title(), left + TEXT_X, top + 14, MUTED, false);
        graphics.drawString(font, counter, left + BOOK_WIDTH - 22 - font.width(counter), top + 14, MUTED, false);

        int y = top + 30;
        Component pageTitle = Component.literal(page.title()).withStyle(ChatFormatting.BOLD);
        for (var line : font.split(pageTitle, TEXT_WIDTH)) {
            graphics.drawString(font, line, left + TEXT_X, y, INK, false);
            y += LINE_HEIGHT;
        }

        ItemStack icon = iconOf(page.icon());
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, left + BOOK_WIDTH / 2 - 8, y);
            y += 20;
        }

        for (var line : font.split(Component.literal(page.text()), TEXT_WIDTH)) {
            if (y > top + BOTTOM_LIMIT) {
                break;
            }
            graphics.drawString(font, line, left + TEXT_X, y, INK, false);
            y += LINE_HEIGHT;
        }
    }

    private static ItemStack iconOf(String id) {
        if (id == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            turn(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            turn(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
