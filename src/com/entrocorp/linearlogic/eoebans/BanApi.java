package com.entrocorp.linearlogic.eoebans;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanApi {
    static Connection admin;
    static Connection login;

    public static void ban(String victim, String sender, String reason, long time) {
        ban(victim, sender, reason, time, admin);
    }

    public static void unban(String victim, CommandSender sender) {
        unban(victim, admin);
    }

    public static void unban(String victim) {
        unban(victim, admin);
    }

    public static BanState banInfo(String victim) {
        return banInfo(victim, admin);
    }

    public static BanState banInfoFromLogin(String victim) {
        return banInfoFromLogin(victim, admin);
    }

    public static void permBan(String victim) {
        permBan(victim, admin);
    }

    static void ban(String victim, String sender, String reason, long time, Connection con) {
        String st = "UPDATE PlayerBans SET Banned='true', BannedBy='" + sender + "', BanTime='" + time + "', Reason='" + 
                reason.replace("'", "") + "' WHERE `Name` = '" + victim + "' ;";
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate(st);
            stmt.close();
        } catch (Exception ex) {
            System.err.println("[BanManager] MySQL-Error: " + ex.getMessage());
        }
    }

    static void unban(String victim, Connection con) {
        String st = "UPDATE PlayerBans SET Banned='false', BannedBy='None', BanTime='0', Reason='', Perm='false' WHERE `Name` = '" + 
                victim + "' ;";
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate(st);
            stmt.close();
        } catch (Exception ex) {
            System.err.println("[BanManager] MySQL-Error: " + ex.getMessage());
        }
    }

    public static void ban(String string, CommandSender sender, boolean perm) {
        long time = 0L;
        string = string.replace("  ", " ");
        String reason = "Hacking? Spamming? Giving out abuse? ";
        String[] args = string.split(" ");
        if (Character.isWhitespace(args[0].toCharArray()[0])) {
            sender.sendMessage(ChatColor.RED + "Ban string is empty!");
            return;
        }
        if (args.length > 1) {
            int c = 1;
            if (isNumeric(args[1])) {
                time = (long)(Double.parseDouble(args[1]) * 60.0D * 60.0D);
                time += System.currentTimeMillis() / 1000L;
                c = 2;
            }
            if (args.length > c)
                reason = "";
            for (int i = c; i < args.length; i++)
                reason = reason + args[i] + " ";
        }
        reason = reason.substring(0, reason.length() - 1);
        if (!sender.hasPermission("BanManager.Permban")) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "You don't have permission to perm ban, Reduced to 24h");
            time = 86400L;
            time += System.currentTimeMillis() / 1000L;
        }
        BanState state = banInfo(args[0]);
        if (state == null) {
            Player p = Bukkit.getPlayer(args[0]);
            if (p == null) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player doesn't exist");
                return;
            }
            state = banInfo(p.getName());
        }
        ban(state.getVictim(), sender.getName(), reason, time);
        if (!perm) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "They have been banned");
        } else {
            permBan(state.getVictim());
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "They have been perm banned");
        }
        String[] messages = { "You were banned by " + sender.getName(), reason, 
                "Buy an unban at " + ChatColor.RED + "EyeOfEnder.com" + ChatColor.RESET, 
                (perm ? "Perm banned" : "Banned") + (time == 0L ? " indefinitely" : new StringBuilder(" for ").append(getTime(time)).toString()) };
        Player p = Bukkit.getPlayerExact(state.getVictim());
        if (p != null)
            p.kickPlayer(StringUtils.join(messages, "\n"));
        else {
            for (Player player : Bukkit.getOnlinePlayers())
                if (player.getAddress().getAddress().getHostAddress().equalsIgnoreCase(args[0]))
                    player.kickPlayer(StringUtils.join(messages, "\n"));
        }
        banInfo(state.getVictim(), admin);
    }

    static String getTime(long number) {
        return "%d days %d hours, %d minutes"
                .replaceFirst("%d", Integer.toString((int)Math.floor((number - System.currentTimeMillis() / 1000L) / 86400L)))
                .replaceFirst("%d", Integer.toString((int)Math.floor((number - System.currentTimeMillis() / 1000L) / 3600L) % 24))
                .replaceFirst("%d", Integer.toString((int)Math.floor((number - System.currentTimeMillis() / 1000L) / 60L) % 60));
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    static BanState banInfo(String victim, Connection con) {
        String st = "SELECT * FROM `PlayerBans` WHERE `Name` = '" + victim + "' ;";
        try {
            Statement stmt = con.createStatement();
            ResultSet r = stmt.executeQuery(st);
            r.last();
            if (r.getRow() == 0) {
                r.close();
                stmt.close();
                return null;
            }
            BanState state = new BanState(r.getString("LastLogin"), r.getString("Name"), r.getString("BannedBy"), r.getString("Reason"), 
                    r.getLong("BanTime"), r.getString("Banned").equalsIgnoreCase("true"), r.getString("Perm").equalsIgnoreCase("true"), r.getInt("ID"));
            r.close();
            stmt.close();
            return state;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static String processLogin(String playername, final String ip, Connection con) {
        try {
            Statement stmt = con.createStatement();
            ResultSet r = stmt.executeQuery("SELECT * FROM `PlayerBans` WHERE `Name` = '" + playername + "' ;");
            r.first();
            if (r.getRow() != 0)
            {
                final BanState status = new BanState(r.getString("LastLogin"), r.getString("Name"), r.getString("BannedBy"), r.getString("Reason"), 
                        r.getLong("BanTime"), r.getString("Banned").equalsIgnoreCase("true"), r.getString("Perm").equalsIgnoreCase("true"), r.getInt("ID"));
                r.close();
                if (!status.getLastLogin().equals(ip)) {
                    stmt.executeUpdate("UPDATE PlayerBans SET LastLogin='" + ip + "' WHERE `ID` = '" + status.getSQLID() + "' ;");
                    Bukkit.getScheduler().scheduleSyncDelayedTask(Bukkit.getPluginManager().getPlugin("BanManager"), 
                            new Runnable() {
                        public void run() {
                            Player p = Bukkit.getPlayerExact(status.getVictim());
                            if ((p != null) && (p.hasPermission("mod.log")))
                                p.sendMessage(ChatColor.DARK_PURPLE + 
                                        "The last IP you have logged in from has changed. Previous: " + 
                                        ChatColor.LIGHT_PURPLE + status.getLastLogin() + ChatColor.DARK_PURPLE + 
                                        ", Current: " + ChatColor.LIGHT_PURPLE + ip + ChatColor.DARK_PURPLE + 
                                        ". Are you secure?");
                        }
                    }
                    , 40L);
                }
                if ((status.getBanTime() != 0L) && (status.getBanTime() < System.currentTimeMillis() / 1000L)) {
                    unban(status.getVictim(), con);
                } else if (status.isBanned()) {
                    String[] messages = { 
                            "You were banned by " + status.getBanner(), 
                            status.getReason(), 
                            "Buy an unban at " + ChatColor.RED + "EyeOfEnder.com" + ChatColor.RESET, 
                            (status.isPermBanned() ? "Perm banned" : "Banned") + (
                                    status.getBanTime() == 0L ? " indefinitely" : new StringBuilder(" for ").append(status.getBanTimeString()).toString()) };
                    return StringUtils.join(messages, "\n");
                }
            } else {
                r.close();
                stmt.execute("INSERT INTO PlayerBans (Name, Banned, BannedBy, BanTime, Reason, LastLogin, Perm) VALUES ('" + 
                        playername + "', 'false', 'None', '0', '', '" + ip + "', 'false')");
            }
            r = stmt.executeQuery("SELECT * FROM `PlayerBans` WHERE `Name` = '" + ip + "' ;");
            r.first();
            if (r.getRow() != 0) {
                BanState status = new BanState(r.getString("LastLogin"), r.getString("Name"), r.getString("BannedBy"), r.getString("Reason"), 
                        r.getLong("BanTime"), r.getString("Banned").equalsIgnoreCase("true"), r.getString("Perm").equalsIgnoreCase("true"), r.getInt("ID"));
                r.close();
                if (!status.getLastLogin().equals(ip))
                    stmt.executeUpdate("UPDATE PlayerBans SET LastLogin='" + playername + "' WHERE `ID` = '" + status.getSQLID() + 
                            "' ;");
                if ((status.getBanTime() != 0L) && (status.getBanTime() < System.currentTimeMillis() / 1000L)) {
                    unban(status.getVictim(), con);
                } else if (status.isBanned())
                {
                    String[] messages = { 
                            "You were banned by " + status.getBanner(), 
                            status.getReason(), 
                            "Buy an unban at " + ChatColor.RED + "EyeOfEnder.com" + ChatColor.RESET, 
                            (status.isPermBanned() ? "Perm banned" : "Banned") + (
                                    status.getBanTime() == 0L ? " indefinitely" : new StringBuilder(" for ").append(status.getBanTimeString()).toString()) };
                    return StringUtils.join(messages, "\n");
                }
            }
            return null;
        }
        catch (Exception ex)
        {
        }
        return null;
    }

    static BanState banInfoFromLogin(String victim, Connection con)
    {
        String st = "SELECT * FROM `PlayerBans` WHERE `LastLogin` = '" + victim + "' ;";
        try {
            Statement stmt = con.createStatement();
            ResultSet r = stmt.executeQuery(st);
            r.last();
            if (r.getRow() == 0) {
                r.close();
                stmt.close();
                return null;
            }
            BanState state = new BanState(r.getString("LastLogin"), r.getString("Name"), r.getString("BannedBy"), r.getString("Reason"), 
                    r.getLong("BanTime"), r.getString("Banned").equalsIgnoreCase("true"), r.getString("Perm").equalsIgnoreCase("true"), r.getInt("ID"));
            r.close();
            stmt.close();
            return state;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void addPlayer(String player, Connection con) {
        try {
            Statement st = con.createStatement();
            st.execute("INSERT INTO PlayerBans (Name, Banned, BannedBy, BanTime, Reason, LastLogin, Perm) VALUES ('" + player + 
                    "', 'false', 'None', '0', '', '', 'false')");
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void permBan(String victim, Connection con) {
        String st = "UPDATE PlayerBans SET Perm='true' WHERE `Name` = '" + victim + "' ;";
        try {
            Statement stmt = con.createStatement();
            stmt.executeUpdate(st);
            stmt.close();
        } catch (Exception ex) {
            System.err.println("[BanManager] MySQL-Error: " + ex.getMessage());
        }
    }

    static void updateLogin(String loginName, String ip) {
        String st = "UPDATE PlayerBans SET LastLogin='" + ip + "' WHERE `Name` = '" + loginName + "' ;";
        try {
            Statement stmt = login.createStatement();
            stmt.executeUpdate(st);
            stmt.close();
        } catch (Exception ex) {
            System.err.println("[BanManager] MySQL-Error: " + ex.getMessage());
        }
    }

    public static void sendState(BanState state, CommandSender sender) {
        if (state == null) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player doesn't exist");
        } else {
            sender.sendMessage(ChatColor.DARK_PURPLE + "Player: " + ChatColor.LIGHT_PURPLE + state.getVictim());
            sender.sendMessage(ChatColor.DARK_PURPLE + "Last login: " + ChatColor.LIGHT_PURPLE + state.getLastLogin());
            sender.sendMessage(ChatColor.DARK_PURPLE + "Banned by: " + ChatColor.LIGHT_PURPLE + (
                    state.getBanner().equals("") ? "No one" : state.getBanner()));
            sender.sendMessage(ChatColor.DARK_PURPLE + "Banned: " + ChatColor.LIGHT_PURPLE + state.isBanned());
            sender.sendMessage(ChatColor.DARK_PURPLE + "Perm banned: " + ChatColor.LIGHT_PURPLE + state.isPermBanned());
            if ((state.isBanned()) || (state.isPermBanned())) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "Ban time: " + ChatColor.LIGHT_PURPLE + state.getBanTimeString());
                sender.sendMessage(ChatColor.DARK_PURPLE + "Reason: " + ChatColor.LIGHT_PURPLE + state.getReason());
            }
        }
    }
}
