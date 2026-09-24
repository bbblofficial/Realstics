package org.housing;

public enum GameMode {

    PLATFORM  ("platform",   "Platform",   "config.yml",     "scoreboard.yml",    "kit-platform.yml"),
    LOWMID    ("lowmid",     "LowMid",     "lowmid.yml",     "sb-lowmid.yml",     "kit-lowmid.yml"),
    ONEWIDE   ("onewide",    "OneWide",    "onewide.yml",    "sb-onewide.yml",    "kit-onewide.yml"),
    BLOCKFIGHT("blockfight", "BlockFight", "blockfight.yml", "sb-blockfight.yml", "kit-blockfight.yml");

    private final String id;
    private final String displayName;
    private final String configFile;
    private final String scoreboardFile;
    private final String kitFile;

    GameMode(String id, String displayName, String configFile,
             String scoreboardFile, String kitFile) {
        this.id = id;
        this.displayName = displayName;
        this.configFile = configFile;
        this.scoreboardFile = scoreboardFile;
        this.kitFile = kitFile;
    }

    public String getId()             { return id; }
    public String getDisplayName()    { return displayName; }
    public String getConfigFile()     { return configFile; }
    public String getScoreboardFile() { return scoreboardFile; }
    public String getKitFile()        { return kitFile; }

    public static GameMode fromId(String id) {
        if (id == null) return null;
        for (GameMode m : values()) {
            if (m.id.equalsIgnoreCase(id)) return m;
        }
        return null;
    }
}