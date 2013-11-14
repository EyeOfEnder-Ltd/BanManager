package com.eyeofender.banmanager;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanApi {

    protected static DatabaseConnecter databaseConnecter;

    protected static void init(BanManager plugin) {
        BanApi.databaseConnecter = new DatabaseConnecter(plugin);
    }

    public static void ban(String name, String banner, String reason, Timestamp expiry) {
        Ban ban = databaseConnecter.getBan(name);
        boolean existing = ban != null;

        if (!existing) ban = new Ban();
        ban.setName(name);
        ban.setBanner(banner);
        ban.setReason(reason);
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
        sender.sendMessage(ChatColor.DARK_PURPLE + "Last login: " + ChatColor.LIGHT_PURPLE + ban.getLastIp());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Banned by: " + ChatColor.LIGHT_PURPLE + ban.getBanner());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Reason: " + ChatColor.LIGHT_PURPLE + ban.getReason());
        sender.sendMessage(ChatColor.DARK_PURPLE + "Banned until: " + ChatColor.LIGHT_PURPLE + formatTimestamp(ban.getExpiry()));
    }

    public static String getBanKickMessage(String name) {
        Ban ban = databaseConnecter.getBan(name);
        if (ban == null) return "";

        String message = "Banned by " + ban.getBanner() + " for " + ban.getReason() + ".\n\n";
        message += "Either wait until:\n";
        message += ChatColor.BLUE + formatTimestamp(ban.getExpiry()) + "\n" + ChatColor.WHITE;
        message += "Or buy an unban at " + ChatColor.AQUA + "EyeOfEnder.com";
        return message;
    }

    public static void updateIP(String name, String ip) {
        Ban ban = databaseConnecter.getBan(name);
        if (ban == null) return;

        ban.setLastIp(ip);
    }

    public static Timestamp getRelative(String time) throws IllegalArgumentException {
        time = time.toLowerCase();

        if (time.contains("forever")) return null;

        int multiple = 1;
        long milliseconds;

        if (time.endsWith("d")) {
            multiple = 1;
        } else if (time.endsWith("w")) {
            multiple = 7;
        } else if (time.endsWith("m")) {
            multiple = 30;
        } else {
            throw new IllegalArgumentException("Invalid time format.");
        }

        time = time.replaceAll("[^\\d.]", "");
        try {
            int value = Integer.parseInt(time);
            milliseconds = TimeUnit.MILLISECONDS.convert(value * multiple, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format.");
        }

        return new Timestamp(System.currentTimeMillis() + milliseconds);
    }

    public static String formatTimestamp(Date date) {
        String formattedDate = "The end of time itself";
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy h:mm:ss a");
            formattedDate = df.format(date);
        }

        return formattedDate;
    }

    public static void updateLogin(String loginName, String ip) {
        Ban ban = databaseConnecter.getBan(loginName);
        if (ban == null) return;
        ban.setLastIp(ip);
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
