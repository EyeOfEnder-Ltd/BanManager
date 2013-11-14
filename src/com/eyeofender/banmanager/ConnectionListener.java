package com.eyeofender.banmanager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
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
        final Player p = event.getPlayer();
        if (p.hasPermission("chat.bypass")) plugin.perms.add(p.getName());
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("IP");
        } catch (IOException localIOException) {
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                p.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            }
        }, 5L);
        try {
            b.close();
            out.close();
        } catch (IOException e) {
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        plugin.perms.remove(p.getName());
    }

}
