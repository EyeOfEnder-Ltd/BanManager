package com.eyeofender.banmanager;

import java.sql.Date;
import java.text.SimpleDateFormat;
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

    public static void ban(String name, String banner, String reason, Date expiry) {
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
        sender.sendMessage(ChatColor.DARK_PURPLE + "Banned until: " + ChatColor.LIGHT_PURPLE + formatDate(ban.getExpiry()));
    }

    public static String getBanKickMessage(String name) {
        Ban ban = databaseConnecter.getBan(name);
        if (ban == null) return "";

        String message = "You were banned by " + ban.getBanner();
        message += " for " + ban.getReason();
        message += "You are banned until:\n";
        message += formatDate(ban.getExpiry());
        message += "\nBuy an unban at " + ChatColor.AQUA + "EyeOfEnder.com";
        return message;
    }

    public static void updateIP(String name, String ip) {
        Ban ban = databaseConnecter.getBan(name);
        if (ban == null) return;

        ban.setLastIp(ip);
    }

    public static Date getRelative(String time) throws IllegalArgumentException {
        time = time.toLowerCase();

        if (time.contains("forever")) return null;
        long milliseconds = 0;
        TimeUnit unit;

        if (time.endsWith("m")) {
            unit = TimeUnit.MINUTES;
        } else if (time.endsWith("h")) {
            unit = TimeUnit.HOURS;
        } else if (time.endsWith("d")) {
            unit = TimeUnit.DAYS;
        } else {
            throw new IllegalArgumentException("Invalid time format.");
        }

        time = time.replaceAll("[^\\d.]", "");
        try {
            int value = Integer.parseInt(time);
            milliseconds = TimeUnit.MILLISECONDS.convert(value, unit);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid time format.");
        }

        return new Date(System.currentTimeMillis() + milliseconds);
    }

    public static String formatDate(Date date) {
        String formattedDate = "The end of time itself";
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            formattedDate = df.format(date.getTime()) + " GMT";
        }

        return formattedDate;
    }

    public static void updateLogin(String loginName, String ip) {
        Ban ban = databaseConnecter.getBan(loginName);
        if (ban == null) return;
        ban.setLastIp(ip);
        databaseConnecter.updateBan(ban);
    }
}
