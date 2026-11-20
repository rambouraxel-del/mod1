package fr.terresvivantes.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.terresvivantes.storage.CrateBlock;
import fr.terresvivantes.storage.CrateBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Rendu de la caisse : l'item stocké sur la face avant, et son nombre en dessous. Client uniquement.
 * Le repère local a son +z vers l'extérieur de la face avant (orientation calculée depuis la direction de face).
 */
public final class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity> {

    /** Décalage de la face avant (le plan se pose juste devant le bloc). */
    private static final double FACE_OFFSET = 0.51;

    private final ItemRenderer itemRenderer;
    private final Font font;

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(CrateBlockEntity crate, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (crate.item() == null || crate.count() <= 0) {
            return;
        }
        BlockState state = crate.getBlockState();
        Direction facing = state.getValue(CrateBlock.FACING);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // Tourne le repère pour que +z pointe vers la face avant.
        pose.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(facing.getStepX(), facing.getStepZ()))));

        // Item au centre de la face.
        pose.pushPose();
        pose.translate(0.0, 0.05, FACE_OFFSET);
        pose.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(new ItemStack(crate.item()), ItemDisplayContext.FIXED, light, overlay, pose,
                buffers, crate.getLevel(), 0);
        pose.popPose();

        // Nombre sous l'item, texte lisible depuis la face avant (axe y inversé pour le texte).
        String text = String.valueOf(crate.count());
        pose.pushPose();
        pose.translate(0.0, -0.36, FACE_OFFSET);
        pose.scale(0.015f, -0.015f, 0.015f);
        float x = -font.width(text) / 2.0f;
        font.drawInBatch(text, x, 0, 0xFFFFFF, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL,
                0, light);
        pose.popPose();

        pose.popPose();
    }
}
