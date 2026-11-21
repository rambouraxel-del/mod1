package fr.terresvivantes.vineyard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Bouture de vigne d'un cépage. Clic droit sur un treillis : le treillis devient une vigne au stade 0.
 */
public final class GrapevineCuttingItem extends Item {

    private final GrapeVariety variety;

    public GrapevineCuttingItem(Properties properties, GrapeVariety variety) {
        super(properties);
        this.variety = variety;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(VineyardModule.TRELLIS.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, VineyardModule.GRAPEVINE.get().defaultBlockState()
                .setValue(GrapevineBlock.VARIETY, variety), Block.UPDATE_ALL);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
