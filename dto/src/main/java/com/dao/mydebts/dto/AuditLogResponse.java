package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Actor;
import com.dao.mydebts.entities.AuditEntry;

import java.util.List;

public class AuditLogResponse {
    private Actor me;

    private List<AuditEntry> entries;

    public Actor getMe() {
        return me;
    }

    public void setMe(Actor me) {
        this.me = me;
    }

    public List<AuditEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<AuditEntry> entries) {
        this.entries = entries;
    }
}
