package com.eyeofender.banmanager;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BungeeListener implements PluginMessageListener {

    private BanManager plugin;

    public BungeeListener(BanManager instance) {
        plugin = instance;
    }

    public void onPluginMessageReceived(String channel, final Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
        try {
            if (in.readUTF().equals("IP")) {
                final String name = player.getName();
                final String ip = in.readUTF();
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    public void run() {
                        if (!BanApi.isBanned(name)) return;

                        BanApi.updateIP(name, ip);
                        player.kickPlayer(BanApi.getBanKickMessage(name));
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
