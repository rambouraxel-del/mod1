package fr.terresvivantes.season;

import fr.terresvivantes.season.logic.Season;
import fr.terresvivantes.season.network.SeasonSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Synchronisation serveur de la saison : envoi aux joueurs au changement de jour,
 * a la connexion, et message de chat a la bascule de saison.
 */
public final class SeasonSync {

    /** Dernier etat envoye a tous les joueurs; null tant qu'aucun etat n'a ete emis depuis le demarrage. */
    private static SeasonSyncPayload lastSent;

    private SeasonSync() {
    }

    /** Etat courant calcule depuis l'overworld. */
    public static SeasonSyncPayload snapshot(ServerLevel overworld) {
        return new SeasonSyncPayload(
                SeasonApi.getSeason(overworld),
                SeasonApi.getDayOfSeason(overworld),
                SeasonApi.getDaysPerSeason());
    }

    /**
     * Compare l'etat courant au dernier envoye. En cas de changement: annonce une nouvelle saison
     * dans le chat si elle a change, puis envoie l'etat a tous les joueurs.
     */
    public static void refresh(MinecraftServer server) {
        SeasonSyncPayload now = snapshot(server.overworld());
        if (now.equals(lastSent)) {
            return;
        }
        if (lastSent != null && lastSent.season() != now.season()) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("terresvivantes.season.changed",
                            Component.translatable(now.season().langKey())),
                    false);
        }
        lastSent = now;
        PacketDistributor.sendToAllPlayers(now);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD)) {
            refresh(level.getServer());
        }
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            PacketDistributor.sendToPlayer(player, snapshot(player.getServer().overworld()));
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        lastSent = null;
    }
}
