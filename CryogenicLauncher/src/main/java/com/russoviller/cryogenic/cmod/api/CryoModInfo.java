package com.russoviller.cryogenic.cmod.api;

/** API 2: information of an installed Launcher Mod (from its cmod.json). */
public final class CryoModInfo {
    private final String id, name, version, author, description, website;
    private final int apiVersion;
    private final boolean enabled, hasGameSide;

    public CryoModInfo(String id, String name, String version, String author, String description,
                       String website, int apiVersion, boolean enabled, boolean hasGameSide) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.website = website;
        this.apiVersion = apiVersion;
        this.enabled = enabled;
        this.hasGameSide = hasGameSide;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getWebsite() { return website; }
    public int getApiVersion() { return apiVersion; }
    public boolean isEnabled() { return enabled; }
    /** True if the mod also runs inside the game ("gameMain" in cmod.json). */
    public boolean hasGameSide() { return hasGameSide; }
}
