//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sevengtz.autochatmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("autochatmod.json");
    private static Config config;

    public static void init() {
        loadConfig();
    }

    public static void loadConfig() {
        if (Files.exists(CONFIG_FILE, new LinkOption[0])) {
            try {
                String content = Files.readString(CONFIG_FILE);
                config = (Config)GSON.fromJson(content, Config.class);
                if (config == null) {
                    config = createDefaultConfig();
                }

                if (config.flaggedTerms == null) {
                    config.flaggedTerms = getDefaultFlaggedTerms();
                }

                if (config.enableDiscordPing == null) {
                    config.enableDiscordPing = true;
                }

                if (config.flaggedPhrases == null) {
                    config.flaggedPhrases = getDefaultFlaggedPhrases();
                }

                if (config.whitelistedTerms == null) {
                    config.whitelistedTerms = getDefaultWhitelistedTerms();
                }

                if (config.whitelistedPhrases == null) {
                    config.whitelistedPhrases = getDefaultWhitelistedPhrases();
                }

                if (config.spamWhitelistPrefixes == null) {
                    config.spamWhitelistPrefixes = getDefaultSpamWhitelistPrefixes();
                }
            } catch (JsonSyntaxException | IOException e) {
                AutoChatMod.LOGGER.error("Failed to load config, using defaults", e);
                config = createDefaultConfig();
            }
        } else {
            config = createDefaultConfig();
        }

        saveConfig();
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            AutoChatMod.LOGGER.error("Failed to save config", e);
        }

    }

    private static Config createDefaultConfig() {
        Config defaultConfig = new Config();
        defaultConfig.webhookUrl = "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE";
        defaultConfig.userMentionId = "YOUR ID HERE";
        defaultConfig.enabled = true;
        defaultConfig.spamDetectionEnabled = true;
        defaultConfig.termDetectionEnabled = true;
        defaultConfig.phraseDetectionEnabled = true;
        defaultConfig.similarityThreshold = 0.8;
        defaultConfig.spamSimilarityThreshold = 0.9;
        defaultConfig.spamMessageCount = 3;
        defaultConfig.spamTimeWindowSeconds = 15;
        defaultConfig.flaggedTerms = getDefaultFlaggedTerms();
        defaultConfig.flaggedPhrases = getDefaultFlaggedPhrases();
        defaultConfig.whitelistedTerms = getDefaultWhitelistedTerms();
        defaultConfig.whitelistedPhrases = getDefaultWhitelistedPhrases();
        defaultConfig.spamWhitelistPrefixes = getDefaultSpamWhitelistPrefixes();
        defaultConfig.alertSound = SoundOption.EXPERIENCE_ORB;
        defaultConfig.alertSoundVolume = 1.0F;
        defaultConfig.alertSoundPitch = 1.0F;
        defaultConfig.enableDiscordPing = false;
        defaultConfig.alertSoundEnabled = true;
        defaultConfig.autoOpenOverlayOnFlag = true;
        defaultConfig.autoOpenPunishGuiOnFlag = false;
        defaultConfig.instantPunishForSpam = false;
        defaultConfig.evidenceScreenshotEnabled = true;
        defaultConfig.evidenceModeratorName = "";
        defaultConfig.xrayAlertThreshold = 3;
        defaultConfig.xrayTimeWindowSeconds = 60;
        defaultConfig.ignoredSystemUsernames = new ArrayList<>(Arrays.asList(
                "Info", "Website", "Store", "Rules", "Discord"
        ));
        defaultConfig.xrayAlertSound = SoundOption.EXPERIENCE_ORB;
        defaultConfig.xrayAlertSoundEnabled = true;
        defaultConfig.reportAlertSound = SoundOption.EXPERIENCE_ORB;
        defaultConfig.reportAlertSoundEnabled = true;
        return defaultConfig;
    }

    public static List<String> getDefaultFlaggedTerms() {
        return new ArrayList(Arrays.asList("nigger", "faggot", "fag", "chink", "tranny", "kys", "slit", "cum", "hitler", "stalin", "child", "doxx", "doxbin", "beaner", "paki", "negro", "queer", "dox", "ddos", "doxxed", "swatted", "ddosed", "cancer", "family", "niger", "frocio", "pd", "pede", "negger", "swat", "suicide"));
    }

    public static List<String> getDefaultFlaggedPhrases() {
        return new ArrayList(Arrays.asList("hang yourself", "kill yourself", "kill urself", "slit your wrists", "hang urself", "kill ur self", "kill your self", "get cancer", "slit ur wrists", "hang ur self", "neck urself", "hope you die", "ching chong", "ur ip", "ur address", "your ip", "your address", "black monkey"));
    }

    public static List<String> getDefaultWhitelistedTerms() {
        return new ArrayList(Arrays.asList("think", "never", "bigger", "digger", "nicer", "china", "pakistan", "rag", "nice", "sweat", "tiger", "chill", "chunk", "thang", "queen"));
    }

    public static List<String> getDefaultWhitelistedPhrases() {
        return new ArrayList(Arrays.asList("Suicide Encouragement"));
    }

    public static List<String> getDefaultSpamWhitelistPrefixes() {
        return new ArrayList(Arrays.asList("[Broadcast]", "[Crates]", "[Spy]", "[System]", "[Server]", "[Auth]", "[*]", "[S]", "[SPAM]", "[FLAGGED]", "X-Ray ▶"));
    }

    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config newConfig) {
        config = newConfig;
        saveConfig();
    }

    public static class Config {
        public String webhookUrl = "";
        public String userMentionId = "";
        public Boolean enableDiscordPing = false;
        public boolean enabled = true;
        public boolean spamDetectionEnabled = true;
        public boolean termDetectionEnabled = true;
        public boolean phraseDetectionEnabled = true;
        public double similarityThreshold = 0.8;
        public double spamSimilarityThreshold = 0.9;
        public int spamMessageCount = 3;
        public int spamTimeWindowSeconds = 15;
        public List<String> flaggedTerms = new ArrayList();
        public List<String> flaggedPhrases = new ArrayList();
        public List<String> whitelistedTerms = new ArrayList();
        public List<String> whitelistedPhrases = new ArrayList();
        public List<String> spamWhitelistPrefixes = new ArrayList();
        public SoundOption alertSound;
        public float alertSoundVolume;
        public float alertSoundPitch;
        public boolean alertSoundEnabled;
        public boolean autoOpenOverlayOnFlag;
        public boolean autoOpenPunishGuiOnFlag;
        public boolean instantPunishForSpam;
        public int hudX;
        public int hudY;
        public int hudWidth;
        public int hudHeight;
        public boolean evidenceScreenshotEnabled;
        public String evidenceModeratorName;
        public int xrayAlertThreshold = 4;
        public int xrayTimeWindowSeconds = 10;
        public List<String> ignoredSystemUsernames = new ArrayList<>();

        public SoundOption xrayAlertSound;
        public boolean xrayAlertSoundEnabled;
        public SoundOption reportAlertSound;
        public boolean reportAlertSoundEnabled;

        public Config() {
            this.alertSound = SoundOption.EXPERIENCE_ORB;
            this.alertSoundVolume = 1.0F;
            this.alertSoundPitch = 1.0F;
            this.alertSoundEnabled = true;
            this.autoOpenOverlayOnFlag = true;
            this.autoOpenPunishGuiOnFlag = false;
            this.instantPunishForSpam = false;
            this.hudX = -1;
            this.hudY = -1;
            this.hudWidth = 250;
            this.hudHeight = 100;
            this.evidenceScreenshotEnabled = true;
        }
    }
}
