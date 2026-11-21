package fr.terresvivantes.guild;

import fr.terresvivantes.economy.EconomyApi;
import fr.terresvivantes.economy.logic.ReputationLogic;
import fr.terresvivantes.guild.logic.GuildLogic;
import fr.terresvivantes.guild.logic.SpecializationLogic;
import fr.terresvivantes.guild.logic.SpecializationLogic.Filiere;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Façade de la guilde (v0.7) : verrouillage des offres par réputation, titres de spécialisation, bonus de qualité.
 * Les autres modules n'appellent que cette classe.
 */
public final class GuildApi {

    private GuildApi() {
    }

    /** Vrai si le villageois exerce la profession de marchand de la guilde. */
    public static boolean isGuildMerchant(Villager villager) {
        return villager.getVillagerData().getProfession() == GuildModule.GUILD_MERCHANT.get();
    }

    /**
     * Bloque (rupture de stock) une offre de la guilde dont le niveau est au-dessus du palier du joueur.
     * Retourne vrai si l'offre a été bloquée. Une offre de niveau accessible n'est pas modifiée.
     */
    public static boolean applyTierLock(MerchantOffer offer, ReputationLogic.Tier tier) {
        int level = GuildTrades.levelOf(offer);
        if (level <= 0 || !GuildLogic.isLocked(level, tier)) {
            return false;
        }
        offer.setToOutOfStock();
        return true;
    }

    /**
     * Applique le verrou de réputation à toutes les offres du villageois, pour le joueur qui l'ouvre.
     * Une offre bloquée reste en rupture jusqu'au réapprovisionnement du villageois (vanilla, environ un jour MC).
     */
    public static void refreshOffers(ServerPlayer player, Villager villager) {
        if (!isGuildMerchant(villager)) {
            return;
        }
        ReputationLogic.Tier tier = EconomyApi.reputationTierOf(player);
        for (MerchantOffer offer : villager.getOffers()) {
            applyTierLock(offer, tier);
        }
    }

    /**
     * Bonus de qualité d'une filière : probabilité (0 à 0.05) qu'une sortie de machine de cette filière soit de qualité
     * +1, pour le joueur qui l'a lancée. 0 sans titre ou si la filière est inconnue.
     *
     * <p>Pour l'intégrateur (module processing, non modifié ici) : la machine appelle cette méthode avec le joueur
     * responsable de la sortie et l'identifiant de filière ({@code "maraicher"}, {@code "arboriculteur"},
     * {@code "eleveur"}, {@code "fromager"}, {@code "apiculteur"}), puis tire {@code random < chance} avant de relever
     * la qualité de la sortie.
     */
    public static float qualityBonusChance(Player player, String filiere) {
        Optional<Filiere> found = SpecializationLogic.byId(filiere);
        if (found.isEmpty() || !hasTitle(player, found.get())) {
            return 0f;
        }
        return SpecializationLogic.QUALITY_BONUS_CHANCE;
    }

    /**
     * Bonus de qualité d'une machine de transformation pour le joueur qui l'a utilisée en dernier : appelé par le
     * module processing à la fin d'une recette. 0 si la machine n'a pas de filière ou si le joueur n'a pas le titre.
     */
    public static float machineQualityBonusChance(Player player, String machineId) {
        return SpecializationLogic.filiereOfMachine(machineId)
                .map(filiere -> qualityBonusChance(player, filiere.id()))
                .orElse(0f);
    }

    /** Unités vendues au marchand dans une filière. */
    public static int units(Player player, Filiere filiere) {
        return player.getData(GuildModule.SPECIALIZATION.get()).getOrDefault(filiere.id(), 0);
    }

    /** Vrai si le joueur a obtenu le titre de la filière. */
    public static boolean hasTitle(Player player, Filiere filiere) {
        return SpecializationLogic.hasTitle(units(player, filiere));
    }

    /**
     * Compte une vente de {@code stack} au marchand dans sa filière, s'il en a une. Une vente qui franchit le seuil
     * donne le titre : message, son et sceau de la guilde.
     */
    public static void recordSale(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        SpecializationLogic.filiereOf(id).ifPresent(filiere -> addUnits(player, filiere, stack.getCount()));
    }

    /** Ajoute des unités à une filière et accorde le titre si le seuil est franchi. */
    static void addUnits(Player player, Filiere filiere, int units) {
        Map<String, Integer> counts = new HashMap<>(player.getData(GuildModule.SPECIALIZATION.get()));
        int before = counts.getOrDefault(filiere.id(), 0);
        int after = SpecializationLogic.add(before, units);
        counts.put(filiere.id(), after);
        player.setData(GuildModule.SPECIALIZATION.get(), Map.copyOf(counts));
        if (SpecializationLogic.reachedThreshold(before, after)) {
            grantTitle(player, filiere);
        }
    }

    private static void grantTitle(Player player, Filiere filiere) {
        player.sendSystemMessage(Component.translatable("terresvivantes.guild.title.gained",
                Component.translatable(titleKey(filiere))));
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        ItemStack seal = new ItemStack(GuildModule.GUILD_SEAL.get());
        if (!player.getInventory().add(seal)) {
            player.drop(seal, false);
        }
    }

    /** Clé de traduction du nom d'une filière. */
    public static String titleKey(Filiere filiere) {
        return "terresvivantes.guild.filiere." + filiere.id();
    }

}
