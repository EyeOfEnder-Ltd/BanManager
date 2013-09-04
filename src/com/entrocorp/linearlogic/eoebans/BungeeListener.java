package com.entrocorp.linearlogic.eoebans;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BungeeListener implements PluginMessageListener {

    private BanManager plugin;

    public BungeeListener(BanManager instance) {
        plugin = instance;
    }

    public void onPluginMessageReceived(String channel, final Player player, byte[] message) {
        if (!channel.equals("BungeeCord"))
            return;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
        try {
            if (in.readUTF().equals("IP")) {
//                final String host = in.readUTF();
//                final int port = in.readInt();
                final String name = player.getName();
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    public void run() {
                        BanState status = BanApi.banInfo(name, BanApi.login);
                        if (status == null) {
                            BanApi.addPlayer(name, BanApi.login);
                            return;
                        }
                        if (!(status.isBanned() || status.isPermBanned()))
                            return;
                        if (status.getBanTime() != 0 && status.getBanTime() <= System.currentTimeMillis() / 1000L) {
                            BanApi.unban(status.getVictim(), BanApi.login);
                            return;
                        }
                        if (BanManager.main.protectedNames.contains(name))
                            return;
                        player.kickPlayer("You were banned by " + status.getBanner() + "\n" + status.getReason() +
                                "\nBuy an unban at " + ChatColor.RED + "EyeOfEnder.com" + ChatColor.RESET + "\n" +
                                (status.isPermBanned() ? "Perm banned" : "Banned") + (status.getBanTime() == 0L ?
                                " indefinitely" : new StringBuilder(" for ").append(status.getBanTimeString()).toString()));
                    }
                });
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
