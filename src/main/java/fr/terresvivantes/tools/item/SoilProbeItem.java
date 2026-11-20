package fr.terresvivantes.tools.item;

import fr.terresvivantes.soil.SoilApi;
import fr.terresvivantes.soil.logic.SoilData;
import fr.terresvivantes.tools.logic.ToolsLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
        return InteractionResult.SUCCESS;
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
