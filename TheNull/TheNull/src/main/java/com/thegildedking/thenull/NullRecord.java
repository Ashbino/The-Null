package com.thegildedking.thenull;

import java.util.UUID;

/** Persistent data for one registered Null player. */
public final class NullRecord {
    private final UUID uuid;
    private String originalName;
    private final String identity;
    private Commander commander;
    private String originalTextureValue;
    private String originalTextureSignature;
    private final long since;

    public NullRecord(UUID uuid, String originalName, String identity, long since) {
        this.uuid = uuid;
        this.originalName = originalName;
        this.identity = identity;
        this.since = since;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getIdentity() {
        return identity;
    }

    public Commander getCommander() {
        return commander;
    }

    public void setCommander(Commander commander) {
        this.commander = commander;
    }

    public String getOriginalTextureValue() {
        return originalTextureValue;
    }

    public String getOriginalTextureSignature() {
        return originalTextureSignature;
    }

    public void setOriginalTexture(String value, String signature) {
        this.originalTextureValue = value;
        this.originalTextureSignature = signature;
    }

    public long getSince() {
        return since;
    }
}
