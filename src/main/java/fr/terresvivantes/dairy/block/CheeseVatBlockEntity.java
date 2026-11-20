package fr.terresvivantes.dairy.block;

import fr.terresvivantes.dairy.DairyModule;
import fr.terresvivantes.processing.ProcessingBlockEntity;
import fr.terresvivantes.processing.ProcessingInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity de la cuve à fromage. Le visuel suit le contenu : lait pendant la première moitié de la caillure,
 * caillé ensuite (et tant que la sortie n'est pas retirée). Le seau est rendu à la fin de chaque recette.
 */
public final class CheeseVatBlockEntity extends ProcessingBlockEntity {

    public CheeseVatBlockEntity(BlockPos pos, BlockState state) {
        super(DairyModule.CHEESE_VAT_BE.get(), pos, state, DairyModule.CHEESE_VAT_TYPE);
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        super.serverTick(level, pos, state);
        BlockState now = level.getBlockState(pos);
        VatContent content = content();
        if (now.hasProperty(CheeseVatBlock.CONTENT) && now.getValue(CheeseVatBlock.CONTENT) != content) {
            level.setBlock(pos, now.setValue(CheeseVatBlock.CONTENT, content), Block.UPDATE_ALL);
        }
    }

    private VatContent content() {
        ProcessingInventory inv = inventory();
        if (!inv.getStackInSlot(0).isEmpty()) {
            boolean halfway = totalTime() > 0 && progress() * 2 >= totalTime();
            return halfway ? VatContent.CURD : VatContent.MILK;
        }
        ItemStack output = inv.getStackInSlot(machine().outputSlot());
        return output.isEmpty() ? VatContent.EMPTY : VatContent.CURD;
    }

    @Override
    protected void craftingTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 40 == 0) {
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 0.9F);
        }
    }
}
