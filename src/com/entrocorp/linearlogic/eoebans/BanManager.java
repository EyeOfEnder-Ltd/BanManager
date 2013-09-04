package com.entrocorp.linearlogic.eoebans;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BanManager extends JavaPlugin implements Listener {
    public String SQL_USER;
    public String SQL_PASS;
    public String SQL_DATA;
    public String SQL_HOST;
    List<String> protectedNames = new ArrayList<String>();

    private int taskID;
    public static BanManager main;
    ConcurrentLinkedQueue<String> perms = new ConcurrentLinkedQueue<String>();

    public void onEnable() {
        main = this;
        saveDefaultConfig();
        SQL_USER = getConfig().getString("sql.user");
        SQL_PASS = getConfig().getString("sql.pass");
        SQL_DATA = getConfig().getString("sql.data");
        SQL_HOST = getConfig().getString("sql.host");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        BanApi.admin = SQLconnect();
        BanApi.login = SQLconnect();
        if (BanApi.admin != null && BanApi.login != null)
            getLogger().info("Successfully connected to SQL backend.");
        loadProtectedNames();
        checkTableExists(BanApi.admin);
        taskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.hasPermission("chat.bypass"))
                        BanManager.main.perms.add(p.getName());
                    else
                        BanManager.main.perms.remove(p.getName());
            }
        }, 0L, 120L);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeListener(this));
    }

    public void onDisable() {
        SQLdisconnect();
        Bukkit.getScheduler().cancelTask(taskID);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player p = event.getPlayer();
        if (p.hasPermission("chat.bypass"))
            this.perms.add(p.getName());
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("IP");
        } catch (IOException localIOException) { }
        getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                p.sendPluginMessage(main, "BungeeCord", b.toByteArray());
            }
        }, 20L);
        try {
            b.close();
            out.close();
        } catch (IOException e) { }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        this.perms.remove(p.getName());
    }

    public void checkTableExists(Connection con) {
        boolean exists = true;
        try {
            DatabaseMetaData dbm = con.getMetaData();

            ResultSet tables = dbm.getTables(null, null, "PlayerBans", null);
            if (!tables.next())
                exists = false;
        } catch (SQLException localSQLException1) {

        } catch (NullPointerException localNullPointerException1) {

        }
        if (!exists) {
            getLogger().info("Could not find PlayerBans table. Creating now...");
            String sta = "CREATE TABLE PlayerBans (ID int(10) unsigned NOT NULL AUTO_INCREMENT, Name varchar(20) NOT NULL, Banned varchar(20) NOT NULL, BannedBy varchar(20) NOT NULL, BanTime int(20) NOT NULL, Reason varchar(200), Perm varchar(20) NOT NULL, LastLogin varchar(200) NOT NULL, PRIMARY KEY (`ID`))";
            try {
                Statement st = con.createStatement();
                st.executeUpdate(sta);
                st.close();
                return;
            } catch (SQLException ex) {
                System.err.println("[BanManager] Error with following query: " + sta);
                System.err.println("[BanManager] MySQL-Error: " + ex.getMessage());
            } catch (NullPointerException ex) {
                System.err.println("[BanManager] Error while performing a query. (NullPointerException)");
            }
        }
        getLogger().info("Found PlayerBans table.");
    }

    public void loadProtectedNames() {
        this.protectedNames.add("enayet123");
        this.protectedNames.add("shazz96");
        this.protectedNames.add("c4d34");
        this.protectedNames.add("TheArcadix");
        this.protectedNames.add("DemandedLogic");
        this.protectedNames.add("LinearLogic");
    }

    public void SQLdisconnect() {
        try {
            System.out.println("[BanManager] Disconnecting from MySQL database...");
            BanApi.admin.close();
            BanApi.login.close();
        } catch (SQLException ex) {
            System.err.println("[BanManager] Error while closing the connection...");
        } catch (NullPointerException ex) {
            System.err.println("[BanManager] Error while closing the connection...");
        }
    }

    public Connection SQLconnect() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String conn = "jdbc:mysql://" + this.SQL_HOST + "/" + this.SQL_DATA;
            return DriverManager.getConnection(conn, this.SQL_USER, this.SQL_PASS);
        } catch (ClassNotFoundException ex) {
            System.err.println("[BanManager] No MySQL driver found!");
        } catch (SQLException ex) {
            System.err.println("[BanManager] Error while fetching MySQL connection!");
        } catch (Exception ex) {
            System.err.println("[BanManager] Unknown error while fetching MySQL connection.");
        }
        return null;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("baninfo")) {
            if ((sender.hasPermission("banmanager.ban")) && (args.length > 0)) {
                BanState state = BanApi.banInfo(args[0]);
                if (state == null) {
                    state = BanApi.banInfoFromLogin(args[0]);
                    if (state == null)
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player doesn't exist");
                    else
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Couldn't find player. But found it under 'last login'");
                }
                if (state != null)
                    BanApi.sendState(state, sender);
            } else if (sender.hasPermission("banmanager.ban")) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "You need to use a playername");
            } else {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "You are not op!");
            }
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("baninfologin")) {
            if ((sender.hasPermission("banmanager.ban")) && (args.length > 0)) {
                BanState state = BanApi.banInfoFromLogin(args[0]);
                if (state == null) {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "Login doesn't exist");
                    return true;
                }
                if (state != null)
                    BanApi.sendState(state, sender);
            } else if (sender.hasPermission("banmanager.ban")) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "You need to use a playername");
            } else {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "You are not op!");
            }
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("ban")) {
            if (sender.hasPermission("banmanager.ban")) {
                if (args.length > 0) {
                    BanState state = BanApi.banInfo(args[0]);
                    if (state == null) {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player doesn't exist");
                        return true;
                    }
                    if (((state.isPermBanned()) || (state.isBanned())) && ((state.getBanTime() == 0L) ||
                            (state.getBanTime() - System.currentTimeMillis() / 1000L * 2L > 0L)) && (!sender.isOp())) {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "You do not have the power to modify this ban");
                        return true;
                    }
                    if ((state.isBanned()) || (state.isPermBanned()))
                        BanApi.sendState(state, sender);
                    BanApi.ban(StringUtils.join(args, " "), sender, false);
                } else {
                    sender.sendMessage(ChatColor.RED + "You need to define a name!");
                }
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("unban")) {
            if (sender.hasPermission("banmanager.ban")) {
                if (args.length > 0) {
                    BanState state = BanApi.banInfo(args[0]);
                    if (state == null) {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "Player doesn't exist");
                        return true;
                    }
                    if (((state.isPermBanned()) || (state.isBanned())) && ((state.getBanTime() == 0L) ||
                            (state.getBanTime() - System.currentTimeMillis() / 1000L * 2L > 0L)) && (!sender.isOp())) {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "You do not have the power to modify this ban");
                        return true;
                    }
                    if ((state.isBanned()) || (state.isPermBanned())) {
                        BanApi.sendState(state, sender);
                        BanApi.unban(state.getVictim());
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + state.getVictim() + " has been unbanned");
                    } else {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + "That player is not banned");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You need to define a player name!");
                }

                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("permban")) {
            if (sender.isOp()) {
                if (args.length > 0)
                    BanApi.ban(StringUtils.join(args, " "), sender, true);
                else
                    sender.sendMessage(ChatColor.RED + "You need to define a player name!");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                return true;
            }
        }
        return true;
    }
}