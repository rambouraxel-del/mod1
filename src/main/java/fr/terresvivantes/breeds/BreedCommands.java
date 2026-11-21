package fr.terresvivantes.breeds;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.terresvivantes.breeds.logic.Breed;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.Optional;

/**
 * Sous-arbre {@code /tv breed set <race>} (opérateur niveau 2) : change la race de la vache ou de la poule regardée.
 * Sert aux tests et à la vitrine. S'ajoute à la racine {@code /tv} par fusion Brigadier.
 */
final class BreedCommands {

    /** Distance maximale du regard pour désigner l'animal. */
    private static final double REACH = 6.0;

    private BreedCommands() {
    }

    static void register(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(BreedCommands::onRegisterCommands);
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tv")
                .then(Commands.literal("breed")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("race", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Breed.values()).map(Breed::id), builder))
                                        .executes(BreedCommands::set))));
        event.getDispatcher().register(root);
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Optional<Breed> breed = Breed.byId(StringArgumentType.getString(context, "race"));
        if (breed.isEmpty()) {
            source.sendFailure(Component.translatable("message.terresvivantes.breeds.command.unknown"));
            return 0;
        }
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, entity -> entity instanceof Animal, REACH);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof Animal animal)) {
            source.sendFailure(Component.translatable("message.terresvivantes.breeds.command.none"));
            return 0;
        }
        if (!BreedsApi.setBreed(animal, breed.get())) {
            source.sendFailure(Component.translatable("message.terresvivantes.breeds.command.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.terresvivantes.breeds.command.set",
                animal.getType().getDescription(), BreedsApi.breedName(breed.get())), true);
        return 1;
    }
}
