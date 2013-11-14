package com.eyeofender.banmanager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BanManager extends JavaPlugin {
    List<String> protectedNames = new ArrayList<String>();

    private int taskID;
    public static BanManager main;
    ConcurrentLinkedQueue<String> perms = new ConcurrentLinkedQueue<String>();

    public void onEnable() {
        main = this;
        BanApi.init(this);

        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        loadProtectedNames();
        taskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.hasPermission("chat.bypass")) BanManager.main.perms.add(p.getName());
                    else
                        BanManager.main.perms.remove(p.getName());
            }
        }, 0L, 120L);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeListener(this));
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTask(taskID);
    }

    public void loadProtectedNames() {
        this.protectedNames.add("enayet123");
        this.protectedNames.add("shazz96");
        this.protectedNames.add("c4d34");
        this.protectedNames.add("limebyte");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("baninfo")) {
            if (args.length > 0) {
                BanApi.sendBanInfo(sender, args[0]);
                if (args[0].equalsIgnoreCase("time")) {
                    sender.sendMessage(BanApi.formatTimestamp(new Date()));
                    sender.sendMessage(BanApi.formatTimestamp(new Timestamp(System.currentTimeMillis())));
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Please specify a player name!");
                return false;
            }
        } else if (cmd.getName().equalsIgnoreCase("ban")) {
            if (args.length > 2) {
                try {
                    Timestamp expiry = BanApi.getRelative(args[1]);
                    BanApi.ban(args[0], sender.getName(), createString(args, 2), expiry);
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "Banned " + args[0] + " until " + BanApi.formatTimestamp(expiry));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + e.getMessage());
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Incorrect arguments!");
                return false;
            }
        } else if (cmd.getName().equalsIgnoreCase("pardon")) {
            if (args.length > 0) {
                if (!BanApi.isBanned(args[0])) {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player '" + args[0] + "' is not banned!");
                    return true;
                }

                BanApi.unban(args[0]);
                sender.sendMessage(ChatColor.LIGHT_PURPLE + args[0] + " has been unbanned");

            } else {
                sender.sendMessage(ChatColor.RED + "Please specify a player name!");
                return false;
            }
        }
        return true;
    }

    private String createString(String[] args, int start) {
        StringBuilder string = new StringBuilder();

        for (int x = start; x < args.length; x++) {
            string.append(args[x]);
            if (x != args.length - 1) {
                string.append(" ");
            }
        }

        return string.toString();
    }

    @Override
    public void installDDL() {
        super.installDDL();
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        return DatabaseConnecter.getDatabaseClasses();
    }
}
