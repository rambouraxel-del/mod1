package fr.terresvivantes.tools.item;

import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.crop.CropApi;
import fr.terresvivantes.pollination.BiodiversityApi;
import fr.terresvivantes.pollination.PollinationApi;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Almanach : clic droit, affiche la saison, le jour, la temperature locale et les cultures optimales. */
public final class AlmanacItem extends Item {

    public AlmanacItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        Season season = SeasonApi.getSeason(level);
        player.sendSystemMessage(Component.translatable("message.terresvivantes.almanac.date",
                Component.translatable(season.langKey()),
                SeasonApi.getDayOfSeason(level),
                SeasonApi.getDaysPerSeason()));

        if (level instanceof ServerLevel server) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.almanac.pollination",
                    Math.round(PollinationApi.score(server, player.blockPosition()) * 100) + " %"));
            player.sendSystemMessage(Component.translatable("message.terresvivantes.almanac.biodiversity",
                    Math.round(BiodiversityApi.index(server, player.blockPosition()) * 100) + " %"));
        }

        float exactTemperature = ClimateApi.temperatureAt(level, player.blockPosition());
        player.sendSystemMessage(Component.translatable("message.terresvivantes.almanac.temperature",
                Math.round(exactTemperature)));

        List<String> crops = CropApi.optimalCropIdsFor(season, exactTemperature);
        if (crops.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.almanac.no_crops"));
        } else {
            MutableComponent names = Component.empty();
            for (int i = 0; i < crops.size(); i++) {
                if (i > 0) {
                    names.append(", ");
                }
                names.append(Component.translatable("item.terresvivantes." + crops.get(i)));
            }
            player.sendSystemMessage(Component.translatable("message.terresvivantes.almanac.crops", names));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
