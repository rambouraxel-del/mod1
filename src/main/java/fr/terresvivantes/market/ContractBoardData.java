package fr.terresvivantes.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.terresvivantes.market.logic.Contract;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tableau d'annonces d'une dimension (v0.7) : contrats ouverts et acceptés, jour du dernier renouvellement,
 * et pénalités de réputation en attente pour les joueurs hors ligne au moment de l'expiration.
 */
public final class ContractBoardData extends SavedData {

    private static final String NAME = "terresvivantes_contracts";

    /** Jour non encore traité : le premier passage à l'aube remplit le tableau. */
    static final long NEVER = Long.MIN_VALUE;

    private static final Codec<Contract> CONTRACT_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("id").forGetter(Contract::id),
            Codec.STRING.fieldOf("product").forGetter(Contract::product),
            Codec.INT.fieldOf("quantity").forGetter(Contract::quantity),
            Codec.INT.fieldOf("min_quality").forGetter(Contract::minQuality),
            Codec.LONG.fieldOf("deadline_day").forGetter(Contract::deadlineDay),
            Codec.INT.fieldOf("reward").forGetter(Contract::reward),
            Codec.INT.fieldOf("reputation").forGetter(Contract::reputation),
            Codec.STRING.xmap(Contract.State::valueOf, Contract.State::name).fieldOf("state")
                    .forGetter(Contract::state),
            Codec.STRING.optionalFieldOf("acceptor", "").forGetter(Contract::acceptor),
            Codec.INT.fieldOf("delivered").forGetter(Contract::delivered)
    ).apply(i, Contract::new));

    private static final SavedData.Factory<ContractBoardData> FACTORY = new SavedData.Factory<>(
            ContractBoardData::new,
            ContractBoardData::load,
            null);

    private final List<Contract> contracts = new ArrayList<>();
    private long nextId = 1L;
    private long lastDay = NEVER;
    private int sequence;
    private final CompoundTag penalties = new CompoundTag();

    public ContractBoardData() {
    }

    private static ContractBoardData load(CompoundTag tag, HolderLookup.Provider registries) {
        ContractBoardData data = new ContractBoardData();
        Tag list = tag.get("contracts");
        if (list != null) {
            CONTRACT_CODEC.listOf().parse(NbtOps.INSTANCE, list).result()
                    .ifPresent(data.contracts::addAll);
        }
        data.nextId = Math.max(1L, tag.getLong("next_id"));
        data.lastDay = tag.contains("last_day") ? tag.getLong("last_day") : NEVER;
        data.sequence = tag.getInt("sequence");
        if (tag.contains("penalties")) {
            data.penalties.merge(tag.getCompound("penalties"));
        }
        return data;
    }

    /** Tableau de la dimension du niveau (créé au premier accès). */
    public static ContractBoardData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CONTRACT_CODEC.listOf().encodeStart(NbtOps.INSTANCE, contracts).result()
                .ifPresent(encoded -> tag.put("contracts", encoded));
        tag.putLong("next_id", nextId);
        tag.putLong("last_day", lastDay);
        tag.putInt("sequence", sequence);
        tag.put("penalties", penalties.copy());
        return tag;
    }

    /** Copie de la liste des contrats (ouverts, acceptés, sans les contrats retirés). */
    public List<Contract> contracts() {
        return List.copyOf(contracts);
    }

    public Optional<Contract> find(long id) {
        return contracts.stream().filter(c -> c.id() == id).findFirst();
    }

    /** Ajoute un contrat au tableau. */
    public void add(Contract contract) {
        contracts.add(contract);
        setDirty();
    }

    /** Remplace le contrat de même identifiant. */
    public void replace(Contract contract) {
        for (int i = 0; i < contracts.size(); i++) {
            if (contracts.get(i).id() == contract.id()) {
                contracts.set(i, contract);
                setDirty();
                return;
            }
        }
    }

    /** Retire le contrat de même identifiant, s'il existe. */
    public void remove(long id) {
        if (contracts.removeIf(c -> c.id() == id)) {
            setDirty();
        }
    }

    /** Identifiant suivant, qui avance d'un. */
    public long nextId() {
        long id = nextId;
        nextId++;
        setDirty();
        return id;
    }

    /** Jour MC du dernier renouvellement ({@link #NEVER} avant le premier). */
    public long lastDay() {
        return lastDay;
    }

    /** Rang du prochain contrat généré dans la journée (sert à la graine). */
    public int nextSequence(long today) {
        if (lastDay != today) {
            sequence = 0;
        }
        int current = sequence;
        sequence++;
        setDirty();
        return current;
    }

    /** Marque le renouvellement du jour {@code today}. */
    public void markRefreshed(long today) {
        lastDay = today;
        sequence = 0;
        setDirty();
    }

    /** Ajoute une pénalité de réputation en attente pour un joueur hors ligne. */
    public void addPenalty(String playerId, int amount) {
        penalties.putInt(playerId, penalties.getInt(playerId) + amount);
        setDirty();
    }

    /** Retire et renvoie la pénalité en attente d'un joueur (0 si aucune). */
    public int takePenalty(String playerId) {
        int amount = penalties.getInt(playerId);
        if (amount > 0) {
            penalties.remove(playerId);
            setDirty();
        }
        return amount;
    }
}
