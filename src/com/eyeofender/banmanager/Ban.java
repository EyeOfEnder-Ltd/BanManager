package com.eyeofender.banmanager;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotNull;

@Entity
@Table(name = "global_bans")
public class Ban {

    @Id
    @Length(max = 16)
    private String name;

    @Column
    private String lastIp;

    @NotNull
    private String banner;

    @NotNull
    private String reason;

    @Column
    private Date expiry;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getExpiry() {
        return expiry;
    }

    public void setExpiry(Date expiry) {
        this.expiry = expiry;
    }

}
