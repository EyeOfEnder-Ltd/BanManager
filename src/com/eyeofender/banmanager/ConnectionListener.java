package com.eyeofender.banmanager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {

    private BanManager plugin;

    public ConnectionListener(BanManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String name = event.getPlayer().getName();
        if (BanApi.isBanned(name)) {
            event.disallow(Result.KICK_BANNED, BanApi.getBanKickMessage(name));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("chat.bypass")) plugin.perms.add(p.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        plugin.perms.remove(p.getName());
    }

}
