package fr.terresvivantes.market;

import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.core.quality.QualityModule;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.economy.EconomyApi;
import fr.terresvivantes.economy.logic.MarketLogic;
import fr.terresvivantes.economy.logic.ReputationLogic;
import fr.terresvivantes.market.logic.Contract;
import fr.terresvivantes.market.logic.ContractGenerator;
import fr.terresvivantes.market.logic.ContractLogic;
import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.FruitTreeDefinitions;
import fr.terresvivantes.season.SeasonApi;
import fr.terresvivantes.season.logic.Season;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import fr.terresvivantes.economy.EconomyModule;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Service des contrats (v0.7) : renouvellement du tableau à l'aube, acceptation, livraison, expiration.
 * Les règles pures sont dans {@code market.logic}; ce service fait le lien avec le monde et les joueurs.
 */
public final class ContractBoard {

    private ContractBoard() {
    }

    /** Appelé à chaque tick de niveau : renouvelle le tableau au premier tick d'un nouveau jour MC (aube). */
    public static void onLevelTick(ServerLevel level) {
        long today = MarketLogic.dayOf(level.getDayTime());
        ContractBoardData data = ContractBoardData.get(level);
        if (data.lastDay() != today) {
            refresh(level, data, today);
        }
    }

    /** Expire les contrats échus, puis complète le tableau à {@link ContractLogic#TARGET_OPEN} contrats ouverts. */
    static void refresh(ServerLevel level, ContractBoardData data, long today) {
        for (Contract contract : data.contracts()) {
            if (ContractLogic.isExpired(today, contract.deadlineDay())) {
                if (contract.state() == Contract.State.ACCEPTED) {
                    penalize(level, contract.acceptor(), ContractLogic.EXPIRY_PENALTY);
                }
                data.remove(contract.id());
            }
        }
        data.markRefreshed(today);

        int missing = ContractLogic.openSlotsToFill(countOpen(data));
        if (missing == 0) {
            return;
        }
        ToIntFunction<String> price = id -> EconomyApi.priceOf(level, new ItemStack(ContractItem.itemOf(id)));
        List<String> seasonal = seasonalProducts(level);
        List<String> others = otherProducts(seasonal);
        ReputationLogic.Tier tier = bestTier(level.getServer());
        long dimensionHash = level.dimension().location().toString().hashCode();
        for (int i = 0; i < missing; i++) {
            long seed = ContractGenerator.seedFor(today, dimensionHash, data.nextSequence(today));
            ContractGenerator.Generated generated = ContractGenerator.generate(seed, tier, today, seasonal, others,
                    price);
            if (generated == null) {
                break;
            }
            data.add(new Contract(data.nextId(), generated.product(), generated.quantity(),
                    generated.minQuality(), generated.deadlineDay(), generated.reward(), generated.reputation(),
                    Contract.State.OPEN, "", 0));
        }
    }

    /** Envoie au joueur les contrats ouverts du tableau de sa dimension : le client ouvre l'écran. */
    public static void openBoard(ServerPlayer player) {
        List<ContractData> open = ContractBoardData.get(player.serverLevel()).contracts().stream()
                .filter(c -> c.state() == Contract.State.OPEN)
                .map(ContractData::of)
                .toList();
        PacketDistributor.sendToPlayer(player, new MarketNetwork.OpenBoardPayload(open));
    }

    /** Le joueur accepte un contrat ouvert : l'item contrat lui est donné avec les données. */
    public static void accept(ServerPlayer player, long id) {
        ServerLevel level = player.serverLevel();
        ContractBoardData data = ContractBoardData.get(level);
        long today = MarketLogic.dayOf(level.getDayTime());
        Optional<Contract> found = data.find(id);
        if (found.isEmpty() || found.get().state() != Contract.State.OPEN
                || ContractLogic.isExpired(today, found.get().deadlineDay())) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.market.unavailable"));
            return;
        }
        Contract accepted = found.get().accepted(player.getUUID().toString());
        data.replace(accepted);
        ItemStack stack = new ItemStack(MarketModule.CONTRACT.get());
        stack.set(MarketModule.CONTRACT_DATA.get(), ContractData.of(accepted));
        giveOrDrop(player, stack);
        player.sendSystemMessage(Component.translatable("message.terresvivantes.market.accepted"));
    }

    /** Livraison avec l'item contrat en main : livre les items du joueur qui satisfont ce contrat. */
    public static int deliverHeld(ServerPlayer player, ItemStack held) {
        ContractData carried = held.get(MarketModule.CONTRACT_DATA.get());
        if (carried == null) {
            return 0;
        }
        ContractBoardData data = ContractBoardData.get(player.serverLevel());
        Optional<Contract> contract = data.find(carried.id());
        if (contract.isEmpty() || contract.get().state() != Contract.State.ACCEPTED) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.market.unavailable"));
            return 0;
        }
        if (!contract.get().acceptor().equals(player.getUUID().toString())) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.market.not_yours"));
            return 0;
        }
        return deliver(player.serverLevel(), player, contract.get());
    }

    /** Livraison sans contrat en main : l'item tenu sert à un contrat accepté par le joueur. */
    public static int deliverMatching(ServerPlayer player, Item item) {
        for (Contract contract : ContractBoardData.get(player.serverLevel()).contracts()) {
            if (contract.state() == Contract.State.ACCEPTED
                    && contract.acceptor().equals(player.getUUID().toString())
                    && ContractItem.itemOf(contract.product()) == item) {
                return deliver(player.serverLevel(), player, contract);
            }
        }
        player.sendSystemMessage(Component.translatable("message.terresvivantes.market.no_match"));
        return 0;
    }

    /**
     * Retire de l'inventaire les items qui satisfont le contrat (qualité suffisante), jusqu'à la quantité restante.
     * Chaque unité livrée compte dans le marché. Contrat complet : paiement, réputation, item retiré.
     * Renvoie le nombre d'unités livrées.
     */
    public static int deliver(ServerLevel level, Player player, Contract contract) {
        ContractBoardData data = ContractBoardData.get(level);
        long today = MarketLogic.dayOf(level.getDayTime());
        if (ContractLogic.isExpired(today, contract.deadlineDay())) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.market.unavailable"));
            return 0;
        }
        Item item = ContractItem.itemOf(contract.product());
        Inventory inventory = player.getInventory();
        int remaining = contract.remaining();
        int moved = 0;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item) || !ContractLogic.meetsQuality(QualityModule.qualityOf(stack), contract.minQuality())) {
                continue;
            }
            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            moved += take;
            remaining -= take;
            for (int unit = 0; unit < take; unit++) {
                EconomyApi.recordSale(level, new ItemStack(item));
            }
        }
        if (moved == 0) {
            player.sendSystemMessage(Component.translatable("message.terresvivantes.market.no_match"));
            return 0;
        }
        inventory.setChanged();
        Contract updated = contract.withDelivered(moved);
        if (updated.remaining() == 0) {
            complete(player, data, updated);
        } else {
            data.replace(updated);
            player.sendSystemMessage(Component.translatable("message.terresvivantes.market.delivered",
                    moved, updated.remaining()));
        }
        return moved;
    }

    private static void complete(Player player, ContractBoardData data, Contract contract) {
        data.remove(contract.id());
        for (ItemStack coins : EconomyApi.copperToCoins(contract.reward())) {
            giveOrDrop(player, coins);
        }
        addReputation(player, contract.reputation());
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ContractData carried = inventory.getItem(slot).get(MarketModule.CONTRACT_DATA.get());
            if (carried != null && carried.id() == contract.id()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        player.sendSystemMessage(Component.translatable("message.terresvivantes.market.completed",
                contract.reward(), contract.reputation()));
    }

    /** Pénalité de réputation : immédiate si le joueur est en ligne, sinon mise en attente jusqu'à sa connexion. */
    private static void penalize(ServerLevel level, String acceptor, int amount) {
        UUID uuid = parse(acceptor);
        if (uuid == null) {
            return;
        }
        ServerPlayer online = level.getServer().getPlayerList().getPlayer(uuid);
        if (online != null) {
            EconomyApi.addReputation(online, -amount);
            online.sendSystemMessage(Component.translatable("message.terresvivantes.market.expired", amount));
        } else {
            ContractBoardData.get(level).addPenalty(acceptor, amount);
        }
    }

    /** À la connexion : applique les pénalités d'expiration survenues hors ligne. */
    public static void onPlayerLogin(ServerPlayer player) {
        String id = player.getUUID().toString();
        for (ServerLevel level : player.getServer().getAllLevels()) {
            int pending = ContractBoardData.get(level).takePenalty(id);
            if (pending > 0) {
                EconomyApi.addReputation(player, -pending);
                player.sendSystemMessage(Component.translatable("message.terresvivantes.market.expired", pending));
            }
        }
    }

    private static int countOpen(ContractBoardData data) {
        return (int) data.contracts().stream().filter(c -> c.state() == Contract.State.OPEN).count();
    }

    /** Produits de saison : cultures dont la saison optimale est la saison courante, fruits mûrs. Triés. */
    private static List<String> seasonalProducts(ServerLevel level) {
        Season season = SeasonApi.getSeason(level);
        TreeSet<String> seasonal = new TreeSet<>();
        for (CropDefinition crop : CropDefinitions.all()) {
            if (crop.optimal().contains(season)) {
                seasonal.add(TerresVivantes.MODID + ":" + crop.id());
            }
        }
        for (FruitTreeDefinition fruit : FruitTreeDefinitions.all()) {
            if (fruit.ripeIn(season)) {
                seasonal.add(fruit.fruit());
            }
        }
        return List.copyOf(seasonal);
    }

    /** Autres produits du mod (transformés, produits animaux) : tous les items du mod hors saison. Triés. */
    private static List<String> otherProducts(List<String> seasonal) {
        return BuiltInRegistries.ITEM.keySet().stream()
                .filter(rl -> TerresVivantes.MODID.equals(rl.getNamespace()))
                .map(ResourceLocation::toString)
                .filter(id -> !seasonal.contains(id))
                .sorted()
                .toList();
    }

    /** Palier du joueur le plus réputé en ligne, ou le palier de base s'il n'y a personne. */
    private static ReputationLogic.Tier bestTier(MinecraftServer server) {
        int best = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            best = Math.max(best, EconomyApi.reputation(player));
        }
        return ReputationLogic.tierOf(best);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    /**
     * Réputation d'un joueur : par l'API d'économie pour un joueur serveur. Un joueur qui n'est pas serveur (faux
     * joueur des GameTests, qui ne peuvent pas être connectés) reçoit le même calcul sur son attachement.
     */
    private static void addReputation(Player player, int delta) {
        if (player instanceof ServerPlayer serverPlayer) {
            EconomyApi.addReputation(serverPlayer, delta);
            return;
        }
        player.setData(EconomyModule.REPUTATION.get(),
                ReputationLogic.adjusted(player.getData(EconomyModule.REPUTATION.get()), delta));
    }

    private static UUID parse(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
