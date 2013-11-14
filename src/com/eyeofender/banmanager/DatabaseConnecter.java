package com.eyeofender.banmanager;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

public class DatabaseConnecter {
    private BanManager plugin;

    public DatabaseConnecter(BanManager plugin) {
        this.plugin = plugin;

        try {
            plugin.getDatabase().find(Ban.class).findRowCount();
        } catch (PersistenceException ex) {
            plugin.getLogger().log(Level.INFO, "Installing database due to first time usage");
            plugin.installDDL();
        }
    }

    public Ban getBan(String name) {
        return plugin.getDatabase().find(Ban.class).where().ieq("name", name).findUnique();
    }

    public void saveBan(Ban ban) {
        plugin.getDatabase().save(ban);
    }

    public void updateBan(Ban ban) {
        plugin.getDatabase().update(ban);
    }

    public void deleteBan(Ban ban) {
        if (ban != null) plugin.getDatabase().delete(ban);
    }

    public boolean isBanned(String name, boolean refresh) {
        Ban ban = getBan(name);
        if (ban == null) return false;

        Date date = new Date(new java.util.Date().getTime());
        Date expiry = ban.getExpiry();

        if (expiry == null || expiry.after(date)) return true;

        plugin.getDatabase().delete(ban);
        return false;
    }

    public static List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<Class<?>>();
        list.add(Ban.class);
        return list;
    }

}
