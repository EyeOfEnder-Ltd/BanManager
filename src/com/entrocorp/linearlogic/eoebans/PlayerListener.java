package com.entrocorp.linearlogic.eoebans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    BanManager main;
    List<Pattern> filterPattern = new LinkedList<Pattern>();
    Map<String, Messages> messages = new HashMap<String, Messages>();
    Map<String, Long> muted = new HashMap<String, Long>();

    public PlayerListener(BanManager main) {
        this.filterPattern = new ArrayList<Pattern>();
        this.filterPattern.add(Pattern.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])"));
        this.filterPattern.add(Pattern.compile("(http://)?(www)?\\S{2,}((\\.com)|(\\.net)|(\\.org)|(\\.co\\.uk)|(\\.tk)|(\\.info)|(\\.es)|(\\.de)|(\\.arpa)|(\\.edu)|(\\.firm)|(\\.int)|(\\.mil)|(\\.mobi)|(\\.nato)|(\\.to)|(\\.fr)|(\\.ms)|(\\.vu)|(\\.eu)|(\\.nl)|(\\.ly))"));
        this.filterPattern.add(Pattern.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\,([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\,([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\,([01]?\\d\\d?|2[0-4]\\d|25[0-5])"));
        this.filterPattern.add(Pattern.compile("(http://)?(www)?\\S{2,}((\\,com)|(\\,net)|(\\,org)|(\\,co\\,uk)|(\\,tk)|(\\,info)|(\\,es)|(\\,de)|(\\,arpa)|(\\,edu)|(\\,firm)|(\\,int)|(\\,mil)|(\\,mobi)|(\\,nato)|(\\,to)|(\\,fr)|(\\,ms)|(\\,vu)|(\\,eu)|(\\,nl)|(\\,ly))"));
        this.main = main;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.messages.remove(event.getPlayer().getName());
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled())
            return;
        if (muted(event.getPlayer()))
            return;
        String message = event.getMessage();
        message = censor(event.getPlayer(), message);
        if (message == null)
            event.setCancelled(true);
        event.setMessage(message);
    }

    public boolean muted(final Player p) {
        if ((this.muted.containsKey(p.getName())) && (((Long)this.muted.get(p.getName())).longValue() < System.currentTimeMillis() / 1000L)) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.main, new Runnable() {
                public void run() {
                    p.sendMessage(ChatColor.RED + "You were muted. Your mute will expire in " + PlayerListener.this.getTime(System.currentTimeMillis() /
                            1000L - ((Long) PlayerListener.this.muted.get(p.getName())).longValue()));
                }
            });
            return true;
        }
        if (this.muted.containsKey(p.getName()))
            this.muted.remove(p.getName());
        return false;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled())
            return;
        if (event.getMessage().startsWith("/me ")) {
            if (muted(event.getPlayer()))
                return;
            String message = event.getMessage().substring(4);
            message = censor(event.getPlayer(), message);
            if (message == null) {
                event.setCancelled(true);
                event.setMessage("/help");
            } else {
                event.setMessage("/me " + message);
            }
        }
    }

    String censor(final Player p, String message) {
        if (this.main.perms.contains(p.getName()))
            return message;
        if (!this.messages.containsKey(p.getName()))
            this.messages.put(p.getName(), new Messages());
        Messages msgs = (Messages)this.messages.get(p.getName());
        if (msgs.isTalkingFast()) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.main, new Runnable() {
                public void run() {
                    p.sendMessage(ChatColor.RED + "Please stop talking so fast");
                }
            });
            return null;
        }
        if (msgs.isSpamming(message)) {
            msgs.setString(message);
            this.messages.put(p.getName(), msgs);
            this.muted.put(p.getName(), Long.valueOf(System.currentTimeMillis() / 1000L + 600L));
            return null;
        }
        msgs.setString(message);
        if (msgs.isAlmostSpamming()) {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.main, new Runnable() {
                public void run() {
                    p.sendMessage(ChatColor.RED + "You are spamming, Please stop or you will be given a timeout");
                }
            });
            return null;
        }
        this.messages.put(p.getName(), msgs);

        for (Pattern pattern : this.filterPattern) {
            Matcher m = pattern.matcher(message.toLowerCase().replaceAll("archergames.net", "").replaceAll(".playeoe.com", "")
                    .replaceAll("www.search-mc.com", "").replaceAll("search-mc.com", "").replaceAll("www.archergames.net", "")
                    .replaceAll("www.mcthefridge.com", "").replaceAll("mcthefridge.com", ""));
            if (m.find()) {
                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.main, new Runnable() {
                    public void run() {
                        p.kickPlayer("Please do not advertise, Advertising is bannable");
                    }
                });
                return null;
            }
        }
        if (message.length() == 1) {
            p.sendMessage(ChatColor.BLUE + "Please don't spam!");
            return null;
        }
        return message; }

    public String getTime(long l) {
        l = Math.abs(l);
        long remainder = l % 3600L; long minutes = remainder / 60L; long seconds = remainder % 60L;
        String time = "";
        if (minutes > 0L) {
            time = time + minutes + " minute";
            if (minutes > 1L)
                time = time + "s";
        }
        if (seconds > 0L) {
            if (minutes > 0L)
                time = time + ", ";
            time = time + seconds + " second";
            if (seconds > 1L)
                time = time + "s";
        }
        return time;
    }

    class Messages {
        String m1 = "Join my server everyone";
        String m2 = "";
        String m3 = "";
        long lastTime = 0L;

        public boolean isAlmostSpamming() {
            if ((equal(this.m1, this.m2)) && (equal(this.m2, this.m3)) && (this.lastTime + 20L > System.currentTimeMillis() / 1000L))
                return true;
            return false;
        }

        public boolean isSpamming(String st) {
            if ((isAlmostSpamming()) && (equal(this.m1, st)))
                return true;
            return false;
        }

        public boolean isTalkingFast() {
            if (this.lastTime + 0.5D > System.currentTimeMillis() / 1000L)
                return true;
            return false;
        }

        public void setString(String st) {
            this.m3 = this.m2;
            this.m2 = this.m1;
            this.m1 = st;
            this.lastTime = (System.currentTimeMillis() / 1000L);
        }

        private boolean equal(String one, String two) {
            if (two.length() > one.length())
                two = two.substring(0, one.length());
            return one.toLowerCase().startsWith(two.toLowerCase());
        }
    }
}
