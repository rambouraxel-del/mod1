package fr.terresvivantes.tools.item;

import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.health.HealthApi;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.soil.SoilApi;
import fr.terresvivantes.soil.logic.RotationModel;
import fr.terresvivantes.soil.logic.SoilData;
import fr.terresvivantes.tools.logic.ToolsLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;

/** Sonde a sol : clic droit sur une terre labouree, affiche la fertilite et l'humidite dans le chat. */
public final class SoilProbeItem extends Item {

    public SoilProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockState(pos).getBlock() instanceof CropBlock) {
            return probeCrop(context, level, pos);
        }
        if (!level.getBlockState(pos).is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player == null) {
            return InteractionResult.PASS;
        }

        SoilData soil = SoilApi.get(level, pos);
        int moisture = level.getBlockState(pos).getValue(FarmBlock.MOISTURE);
        send(player, "message.terresvivantes.soil_probe.nitrogen", soil.n());
        send(player, "message.terresvivantes.soil_probe.phosphorus", soil.p());
        send(player, "message.terresvivantes.soil_probe.potassium", soil.k());
        send(player, "message.terresvivantes.soil_probe.organic", soil.organic());
        player.sendSystemMessage(Component.translatable("message.terresvivantes.soil_probe.moisture",
                colored(String.valueOf(moisture), ToolsLogic.moistureTier(moisture)),
                String.valueOf(ToolsLogic.MAX_MOISTURE)));
        sendRotation(player, level, pos);
        return InteractionResult.SUCCESS;
    }

    /** Culture visée : santé (stress, maladie, immunité), module health. */
    private static InteractionResult probeCrop(UseOnContext context, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        HealthApi.report(player, level, pos);
        return InteractionResult.SUCCESS;
    }

    /** Dernière culture récoltée sur la terre, son nombre de répétitions, et l'effet de rotation attendu. */
    private static void sendRotation(Player player, Level level, BlockPos pos) {
        String last = SoilApi.getLastCrop(level, pos);
        if (last == null) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.soil_probe.last_crop_none"));
            return;
        }
        int repeat = SoilApi.getRepeatCount(level, pos);
        player.sendSystemMessage(Component.translatable("message.terresvivantes.soil_probe.last_crop",
                cropName(last), String.valueOf(repeat)));
        boolean legume = CropDefinitions.byId(last).map(CropDefinition::legume).orElse(false);
        boolean any = false;
        if (repeat >= RotationModel.MONOCULTURE_REPEAT) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.soil_probe.rotation_monoculture"));
            any = true;
        }
        if (legume) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.soil_probe.rotation_legume"));
            any = true;
        }
        if (!any) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.soil_probe.rotation_none"));
        }
    }

    /** Nom affiché d'une culture : culture du mod par son id, culture vanilla par son identifiant de bloc. */
    private static Component cropName(String cropId) {
        if (cropId.contains(":")) {
            return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(cropId))
                    .map(block -> (Component) block.getName())
                    .orElse(Component.literal(cropId));
        }
        return Component.translatable("block.terresvivantes." + cropId + "_crop");
    }

    private static void send(Player player, String key, int value) {
        player.sendSystemMessage(Component.translatable(key,
                colored(String.valueOf(value), ToolsLogic.nutrientTier(value))));
    }

    private static Component colored(String text, ToolsLogic.Tier tier) {
        ChatFormatting color = switch (tier) {
            case LOW -> ChatFormatting.RED;
            case MEDIUM -> ChatFormatting.YELLOW;
            case HIGH -> ChatFormatting.GREEN;
        };
        return Component.literal(text).withStyle(color);
    }
}
