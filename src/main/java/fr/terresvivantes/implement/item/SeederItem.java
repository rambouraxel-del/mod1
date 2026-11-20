package fr.terresvivantes.implement.item;

import fr.terresvivantes.implement.logic.ImplementsLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Semoir : clic droit sur terre labouree plante les graines de la main secondaire sur une zone 3x3,
 * ou sur une ligne de 5 cases si le joueur est accroupi. Une graine et une durabilite par case plantee.
 * La plantation passe par {@link BlockItem#place}, donc par {@code setPlacedBy} (notifie les plant listeners).
 */
public final class SeederItem extends Item {

    private static final TagKey<Item> SEEDS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "seeds"));

    public SeederItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clicked = context.getClickedPos();
        if (player == null || !level.getBlockState(clicked).is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }
        ItemStack seeds = player.getOffhandItem();
        if (!isPlantableSeed(seeds)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.terresvivantes.seeder.no_seeds"), true);
            }
            return InteractionResult.PASS;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }

        int[][] offsets = player.isShiftKeyDown()
                ? ImplementsLogic.row(player.getDirection().getStepX(), player.getDirection().getStepZ())
                : ImplementsLogic.square(ImplementsLogic.GRID_RADIUS);
        int planted = 0;
        for (int[] offset : offsets) {
            if (seeds.isEmpty()) {
                break;
            }
            BlockPos soil = clicked.offset(offset[0], 0, offset[1]);
            if (plant(level, soil, player, seeds)) {
                planted++;
            }
        }
        if (planted == 0) {
            return InteractionResult.PASS;
        }
        level.playSound(null, clicked, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
        context.getItemInHand().hurtAndBreak(planted, player, slotFor(context.getHand()));
        return InteractionResult.SUCCESS;
    }

    /** Pose une graine au-dessus d'une terre labouree libre. Consomme une graine si la pose reussit. */
    private static boolean plant(Level level, BlockPos soil, Player player, ItemStack seeds) {
        if (!level.getBlockState(soil).is(Blocks.FARMLAND) || !level.getBlockState(soil.above()).isAir()) {
            return false;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
        BlockPlaceContext placeContext = new BlockPlaceContext(player, InteractionHand.OFF_HAND, seeds, hit);
        int before = seeds.getCount();
        if (!((BlockItem) seeds.getItem()).place(placeContext).consumesAction()) {
            return false;
        }
        // BlockItem.place retire déjà la graine en survie : on ne la retire à la main que si elle est restée.
        if (seeds.getCount() == before && !player.getAbilities().instabuild) {
            seeds.shrink(1);
        }
        return true;
    }

    /** Graine plantable : BlockItem dont le bloc est une culture, ou item du tag c:seeds lie a un bloc. */
    static boolean isPlantableSeed(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return blockItem.getBlock() instanceof CropBlock || stack.is(SEEDS);
    }

    private static EquipmentSlot slotFor(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }
}
