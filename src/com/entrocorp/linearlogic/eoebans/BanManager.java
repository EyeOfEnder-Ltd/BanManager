package com.entrocorp.linearlogic.eoebans;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BanManager extends JavaPlugin implements Listener, PluginMessageListener {
    public String SQL_USER;
    public String SQL_PASS;
    public String SQL_DATA;
    public String SQL_HOST;
    List<String> protectedNames = new ArrayList<String>();
    int current = 0;

    int sched = 0;
    public static BanManager main;
    ConcurrentLinkedQueue<String> perms = new ConcurrentLinkedQueue<String>();

    public void onEnable() {
        main = this;
        saveDefaultConfig();
        SQL_USER = getConfig().getString("sql.user");
        SQL_PASS = getConfig().getString("sql.pass");
        SQL_DATA = getConfig().getString("sql.data");
        SQL_HOST = getConfig().getString("sql.host");
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(this, this);
        manager.registerEvents(new PlayerListener(this), this);
        BanApi.admin = SQLconnect();
        BanApi.login = SQLconnect();
        if (BanApi.admin != null && BanApi.login != null)
            getLogger().info("Successfully connected to SQL backend.");
        protectedNames();
        checkTableExists(BanApi.admin);
        sched = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.hasPermission("chat.bypass"))
                        BanManager.main.perms.add(p.getName());
                    else
                        BanManager.main.perms.remove(p.getName());
            }
        }
        , 0L, 120L);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
    }

    public boolean isBungee(InetAddress address) {
        return (address.getHostAddress().equals(Bukkit.getIp())) || (address.getHostAddress().equalsIgnoreCase("209.188.4.82"));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("chat.bypass"))
            this.perms.add(p.getName());
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("IP");
        } catch (IOException localIOException) { }
        p.sendPluginMessage(BanManager.main, "BungeeCord", b.toByteArray());
        try {
            b.close();
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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

    public void onDisable() {
        SQLdisconnect();
        Bukkit.getScheduler().cancelTask(this.sched);
    }

    public void protectedNames() {
        this.protectedNames.add("enayet123");
        this.protectedNames.add("shazz96");
        this.protectedNames.add("c4d34");
        this.protectedNames.add("TheArcadix");
        this.protectedNames.add("DemandedLogic");
        this.protectedNames.add("LinearLogic");
    }

    private long checkBanned(Connection con, AsyncPlayerPreLoginEvent event, BanState status) {
        if ((!status.isBanned()) && (!status.isPermBanned()))
            return -1L;
        if ((status.getBanTime() != 0L) && (status.getBanTime() < System.currentTimeMillis() / 1000L)) {
            BanApi.unban(status.getVictim(), con);
            return -1L;
        }
        String[] messages = {
                "You were banned by " + status.getBanner(), status.getReason(), "Buy an unban at " + ChatColor.RED +
                "EyeOfEnder.com" + ChatColor.RESET, (status.isPermBanned() ? "Perm banned" : "Banned") + (status.getBanTime() == 0L ?
                        " indefinitely" : new StringBuilder(" for ").append(status.getBanTimeString()).toString())
        };
        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
        event.setKickMessage(StringUtils.join(messages, "\n"));
        return status.getBanTime();
    }

    public void oldPlayerLogin(final AsyncPlayerPreLoginEvent event) {
        Connection con = SQLconnect();
        if (con == null)
            return;
        long id = -1L;
        BanState status = BanApi.banInfo(event.getName(), con);
        if (status == null)
            BanApi.addPlayer(event.getName(), con);
        else
            id = checkBanned(con, event, status);
        if (!isBungee(event.getAddress())) {
            BanApi.updateLogin(event.getName(), event.getAddress().getHostAddress());
            status = BanApi.banInfo(event.getAddress().getHostAddress(), con);
            if (status != null)
                BanApi.updateLogin(event.getAddress().getHostAddress(), event.getName());
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                public void run() {
                    Player player = Bukkit.getPlayerExact(event.getName());
                    if (player != null) {
                        ByteArrayOutputStream b = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(b);
                        try {
                            out.writeUTF("IP");
                        } catch (IOException localIOException) {
                        }
                        player.sendPluginMessage(BanManager.main, "BungeeCord", b.toByteArray());
                    }
                }
            }
            , 40L);
        }
        if (this.protectedNames.contains(event.getName()))
            event.allow();
        if (id == 0L) {
            this.current -= 1;
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        if ((status != null) && (status.getBanTime() > id))
            checkBanned(con, event, status);
        if (this.protectedNames.contains(event.getName()))
            event.allow();
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void playerLogin(final AsyncPlayerPreLoginEvent event) {
        System.out.println("0");
        if (BanApi.login == null) {
            return;
        }
        Connection con = BanApi.login;
        if (BanManager.main.isBungee(event.getAddress())) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(BanManager.main, new Runnable() {
                public void run() {
                    Player player = Bukkit.getPlayerExact(event.getName());
                    if (player != null) {
                        ByteArrayOutputStream b = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(b);
                        try {
                            out.writeUTF("IP");
                        } catch (IOException localIOException) {
                        }
                        player.sendPluginMessage(BanManager.main, "BungeeCord", b.toByteArray());
                    }
                }
            }
            , 40L);
        }
        if (BanManager.main.protectedNames.contains(event.getName())) {
            event.allow();
        } else {
            System.out.println("1");
            String banMessage = BanApi.processLogin(event.getName(), event.getAddress().getHostAddress(), con);
            if (banMessage != null)
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banMessage);
        }
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
        } else if (cmd.getName().equalsIgnoreCase("banip")) {
            if (sender.hasPermission("banmanager.ban")) {
                if (args.length > 0) {
                    BanState state = BanApi.banInfo(args[0]);
                    if (state == null) {
                        BanApi.addPlayer(args[0], BanApi.admin);
                        state = BanApi.banInfo(args[0]);
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
                    sender.sendMessage(ChatColor.RED + "You need to define a IP!");
                }
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("permbanip")) {
            if (sender.isOp()) {
                if (args.length > 0) {
                    BanState state = BanApi.banInfo(args[0]);
                    if (state == null) {
                        BanApi.addPlayer(args[0], BanApi.admin);
                        state = BanApi.banInfo(args[0]);
                    }
                    if ((state.isBanned()) || (state.isPermBanned()))
                        BanApi.sendState(state, sender);
                    BanApi.ban(StringUtils.join(args, " "), sender, true);
                } else {
                    sender.sendMessage(ChatColor.RED + "You need to define a IP!");
                }
                return true;
            }
            return true;
        }
        return true;
    }

    public void onPluginMessageReceived(String channel, final Player player, byte[] message) {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
        try {
            if (in.readUTF().equals("IP")) {
                final String host = in.readUTF();
                final String name = player.getName();
                Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
                    public void run() {
                        AsyncPlayerPreLoginEvent event = null;
                        try {
                            event = new AsyncPlayerPreLoginEvent(name, InetAddress.getByName(host));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                            return;
                        }
                        long id = -1L;
                        BanState status = BanApi.banInfo(name, BanApi.login);
                        if (status == null)
                            BanApi.addPlayer(name, BanApi.login);
                        else
                            id = BanManager.main.checkBanned(BanApi.login, event, status);
                        BanApi.updateLogin(event.getName(), event.getAddress().getHostAddress());
                        status = BanApi.banInfo(event.getAddress().getHostAddress(), BanApi.login);
                        if (status != null) {
                            BanApi.updateLogin(event.getAddress().getHostAddress(), event.getName());
                        }
                        if (BanManager.main.protectedNames.contains(event.getName()))
                            event.allow();
                        if (id == 0L) {
                            BanManager.main.current -= 1;
                            return;
                        }
                        if ((status != null) && (status.getBanTime() > id))
                            BanManager.main.checkBanned(BanApi.login, event, status);
                        if (BanManager.main.protectedNames.contains(event.getName()))
                            event.allow();
                        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
                            player.kickPlayer(event.getKickMessage());
                    }
                });
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}