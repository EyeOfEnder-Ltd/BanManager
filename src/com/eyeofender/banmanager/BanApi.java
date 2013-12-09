package com.eyeofender.banmanager;

import java.sql.Date;
import java.text.SimpleDateFormat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanApi {

    protected static DatabaseConnecter databaseConnecter;

    protected static void init(BanManager plugin) {
        BanApi.databaseConnecter = new DatabaseConnecter(plugin);
    }

    public static void ban(String name, String byWhom, String reason, Date date, Date expiry) {
        Ban ban = databaseConnecter.getBan(name);
        boolean existing = ban != null;

        if (!existing) ban = new Ban();
        ban.setName(name);
        ban.setByWhom(byWhom);
        ban.setReason(reason);
        ban.setDate(date);
        ban.setExpiry(expiry);

        if (existing) {
            databaseConnecter.updateBan(ban);
        } else {
            databaseConnecter.saveBan(ban);
        }

        Player player = Bukkit.getPlayerExact(name);
        if (player != null) player.kickPlayer(getBanKickMessage(name));
    }

    public static void unban(String name) {
        databaseConnecter.deleteBan(databaseConnecter.getBan(name));
    }

    public static boolean isBanned(String name) {
        return databaseConnecter.isBanned(name, true);
    }

    public static void sendBanInfo(CommandSender sender, String name) {
        Ban ban = databaseConnecter.getBan(name);

        if (ban == null) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player is not banned.");
            return;
        }

        sender.sendMessage(ChatColor.DARK_PURPLE + "Player: " + ChatColor.LIGHT_PURPLE + ban.getName());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Banned by: " + ChatColor.LIGHT_PURPLE + ban.getByWhom());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Reason: " + ChatColor.LIGHT_PURPLE + ban.getReason());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Banned on: " + ChatColor.LIGHT_PURPLE + formatTimestamp(ban.getDate()));
        sender.sendMessage(ChatColor.DARK_PURPLE + "Banned until: " + ChatColor.LIGHT_PURPLE + formatTimestamp(ban.getExpiry()));
    }

    public static String getBanKickMessage(String name) {
        Ban ban = databaseConnecter.getBan(name);
        if (ban == null) return "";

        String message = "Banned by " + ban.getByWhom() + " for " + ban.getReason() + ".\n\n";
        message += "Either wait until:\n";
        message += ChatColor.BLUE + formatTimestamp(ban.getExpiry()) + "\n" + ChatColor.WHITE;
        message += "Or buy an unban at " + ChatColor.AQUA + "EyeOfEnder.com";
        return message;
    }

    public static Date getRelative(Date date, String addition) {
        addition = addition.toLowerCase();

        if (addition.contains("forever")) return null;

        int m = 0;
        int w = 0;
        int d = 0;
        int h = 0;
        int min = 0;

        String[] parts = addition.split(",");
        for (String part : parts) {
            String[] spl = part.split(":");
            if (addition.contains("m:")) {
                m = Integer.parseInt(spl[1]);
            } else if (addition.contains("w:")) {
                w = Integer.parseInt(spl[1]);
            } else if (addition.contains("d:")) {
                d = Integer.parseInt(spl[1]);
            } else if (addition.contains("h:")) {
                h = Integer.parseInt(spl[1]);
            } else if (addition.contains("min:")) {
                min = Integer.parseInt(spl[1]);
            }
        }

        String[] spl = addition.split(":");
        if (addition.contains("m:")) {
            m = Integer.parseInt(spl[1]);
        } else if (addition.contains("w:")) {
            w = Integer.parseInt(spl[1]);
        } else if (addition.contains("d:")) {
            d = Integer.parseInt(spl[1]);
        } else if (addition.contains("h:")) {
            h = Integer.parseInt(spl[1]);
        } else if (addition.contains("min:")) {
            min = Integer.parseInt(spl[1]);
        }

        long time = 0;
        if (m > 0) {
            time = m * 2419200;
        }
        if (w > 0) {
            time = time + (w * 604800);
        }
        if (d > 0) {
            time = time + (d * 86400);
        }
        if (h > 0) {
            time = time + (h * 3600);
        }
        if (min > 0) {
            time = time + (min * 60);
        }

        long milliseconds = time * 1000;
        return new Date(date.getTime() + milliseconds);
    }

    public static String formatTimestamp(Date date) {
        String formattedDate = "The end of time itself";
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy h:mm:ss a");
            formattedDate = df.format(new Date(date.getTime()));
        }

        return formattedDate;
    }

    public static void updateLogin(String loginName, String ip) {
        Ban ban = databaseConnecter.getBan(loginName);
        if (ban == null) return;
        databaseConnecter.updateBan(ban);
    }

    public static void refreshAll() {
        Date date = new Date(new java.util.Date().getTime());
        for (Ban ban : databaseConnecter.getBans()) {
            Date expiry = ban.getExpiry();
            if (expiry == null || expiry.after(date)) unban(ban.getName());
        }
    }

    public static void sendBanList(CommandSender sender) {
        refreshAll();

        StringBuilder message = new StringBuilder();
        Ban[] banlist = databaseConnecter.getBans().toArray(new Ban[0]);

        for (int x = 0; x < banlist.length; x++) {
            if (x != 0) {
                if (x == banlist.length - 1) {
                    message.append(" and ");
                } else {
                    message.append(", ");
                }
            }
            message.append(banlist[x].getName());
        }

        sender.sendMessage(ChatColor.DARK_PURPLE + "There are " + banlist.length + " total banned players:");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + message.toString());
    }
}
