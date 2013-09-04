package com.entrocorp.linearlogic.eoebans;

public class BanState {

    private String ip;
    private String victim;
    private String banner;
    private String reason;
    private long banTime;
    private boolean banned;
    private boolean permBanned;

    private int sqlID;

    BanState(String lastLogin, String victim, String banner, String reason, long banTime,
            boolean banned, boolean permBanned, int sqlID) {
        ip = lastLogin;
        this.victim = victim;
        this.banner = banner;
        this.reason = reason;
        this.banTime = banTime;
        this.banned = banned;
        this.permBanned = permBanned;
        this.sqlID = sqlID;
    }

    public int getSQLID() {
        return sqlID;
    }

    public String getVictim() {
        return victim;
    }

    public String getBanner() {
        return banner;
    }

    public String getReason() {
        return reason;
    }

    public String getLastLogin() {
        return ip;
    }

    public boolean isBanned() {
        return banned || permBanned;
    }

    public boolean isPermBanned() {
        return permBanned;
    }

    public long getBanTime() {
        return banTime;
    }

    public String getBanTimeString() {
        if (banTime > 0L)
            return "%d days %d hours, %d minutes"
                    .replaceFirst("%d", Integer.toString((int) Math.floor((banTime - System.currentTimeMillis() / 1000L) / 86400L)))
                    .replaceFirst( "%d", Integer.toString((int) Math.floor((banTime - System .currentTimeMillis() / 1000L) / 3600L) % 24))
                    .replaceFirst( "%d", Integer.toString((int) Math.floor((banTime - System .currentTimeMillis() / 1000L) / 60L) % 60));
        return "Banned indefinitely";
    }
}
