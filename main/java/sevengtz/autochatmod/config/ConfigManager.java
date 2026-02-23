package sevengtz.autochatmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages loading, saving, and accessing mod configuration.
 * Provides singleton-style access to the current configuration.
 */
public final class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("autochatmod.json");

    private static ModConfig config;

    // Backward compatibility alias
    public static class Config extends ModConfig {
    }

    private ConfigManager() {
        // Utility class - prevent instantiation
    }

    /**
     * Initializes the configuration system.
     * Should be called during mod initialization.
     */
    public static void init() {
        loadConfig();
    }

    /**
     * Loads the configuration from disk.
     * Creates default configuration if none exists.
     */
    public static void loadConfig() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                String content = Files.readString(CONFIG_FILE);
                config = GSON.fromJson(content, ModConfig.class);

                if (config == null) {
                    config = createDefaultConfig();
                }

                ensureListsInitialized();

            } catch (IOException | JsonSyntaxException e) {
                LOGGER.error("Failed to load config, using defaults", e);
                config = createDefaultConfig();
            }
        } else {
            config = createDefaultConfig();
        }
        saveConfig();
    }

    /**
     * Ensures all list fields are properly initialized.
     */
    private static void ensureListsInitialized() {
        if (config.customBotUrl == null || config.customBotUrl.isEmpty())
            config.customBotUrl = "http://node63.lunes.host:3242";
        if (config.flaggedTerms == null)
            config.flaggedTerms = getDefaultFlaggedTerms();
        if (config.enableDiscordPing == null)
            config.enableDiscordPing = true;
        if (config.flaggedPhrases == null)
            config.flaggedPhrases = getDefaultFlaggedPhrases();
        if (config.whitelistedTerms == null)
            config.whitelistedTerms = getDefaultWhitelistedTerms();
        if (config.whitelistedPhrases == null)
            config.whitelistedPhrases = getDefaultWhitelistedPhrases();
        if (config.spamWhitelistPrefixes == null)
            config.spamWhitelistPrefixes = getDefaultSpamWhitelistPrefixes();
        if (config.ignoredSystemUsernames == null)
            config.ignoredSystemUsernames = new ArrayList<>();
    }

    /**
     * Saves the current configuration to disk.
     */
    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            String json = GSON.toJson(config);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    /**
     * Creates a new configuration with default values.
     * 
     * @return A new ModConfig with defaults
     */
    private static ModConfig createDefaultConfig() {
        ModConfig defaultConfig = new ModConfig();
        defaultConfig.webhookUrl = "https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE";
        defaultConfig.userMentionId = "YOUR ID HERE";
        defaultConfig.customBotUrl = "http://node63.lunes.host:3242";
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
        return defaultConfig;
    }

    // Default value providers

    public static List<String> getDefaultFlaggedTerms() {
        return new ArrayList<>(Arrays.asList(
                "nigger", "faggot", "fag", "chink", "tranny", "kys", "slit", "cum", "hitler",
                "stalin", "child", "doxx", "doxbin", "beaner", "paki", "negro", "queer", "dox",
                "ddos", "doxxed", "swatted", "ddosed", "cancer", "family", "niger", "frocio",
                "pd", "pede", "negger", "swat", "suicide"));
    }

    public static List<String> getDefaultFlaggedPhrases() {
        return new ArrayList<>(Arrays.asList(
                "hang yourself", "kill yourself", "kill urself", "slit your wrists", "hang urself",
                "kill ur self", "kill your self", "get cancer", "slit ur wrists", "hang ur self",
                "neck urself", "hope you die", "ching chong", "ur ip", "ur address", "your ip",
                "your address", "black monkey"));
    }

    public static List<String> getDefaultWhitelistedTerms() {
        return new ArrayList<>(Arrays.asList(
                "think", "never", "bigger", "digger", "nicer", "china", "pakistan", "rag",
                "nice", "sweat", "tiger", "chill", "chunk", "thang", "queen"));
    }

    public static List<String> getDefaultWhitelistedPhrases() {
        return new ArrayList<>(Arrays.asList("Suicide Encouragement"));
    }

    public static List<String> getDefaultSpamWhitelistPrefixes() {
        return new ArrayList<>(Arrays.asList(
                "[Broadcast]", "[Crates]", "[Spy]", "[System]", "[Server]", "[Auth]", "[*]", "[S]", "[SPAM]",
                "[FLAGGED]", "X-Ray ▶"));
    }

    /**
     * Gets the current configuration.
     * 
     * @return The current ModConfig (or Config for compatibility)
     */
    public static ModConfig getConfig() {
        return config;
    }

    /**
     * Sets a new configuration and saves it.
     * 
     * @param newConfig The new configuration to use
     */
    public static void setConfig(ModConfig newConfig) {
        config = newConfig;
        saveConfig();
    }
}
