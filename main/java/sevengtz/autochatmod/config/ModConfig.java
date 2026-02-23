package sevengtz.autochatmod.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration data class holding all mod settings.
 * This is the serialized/deserialized configuration object.
 */
public class ModConfig {

    // Discord Integration
    public String webhookUrl = ""; // Fallback/General (Standard Discord Webhook)
    public String customBotUrl = "http://node63.lunes.host:3242";
    public boolean useCustomBot = true; // Default to true for staff team
    public String userMentionId = "";
    public Boolean enableDiscordPing = false;

    // General Settings
    public boolean enabled = true;
    public boolean spamDetectionEnabled = true;
    public boolean termDetectionEnabled = true;
    public boolean phraseDetectionEnabled = true;

    // Detection Thresholds
    public double similarityThreshold = 0.8;
    public double spamSimilarityThreshold = 0.9;
    public int spamMessageCount = 3;
    public int spamTimeWindowSeconds = 15;

    // Term Lists
    public List<String> flaggedTerms = new ArrayList<>();
    public List<String> flaggedPhrases = new ArrayList<>();
    public List<String> whitelistedTerms = new ArrayList<>();
    public List<String> whitelistedPhrases = new ArrayList<>();
    public List<String> spamWhitelistPrefixes = new ArrayList<>();
    public List<String> ignoredSystemUsernames = new ArrayList<>();

    // Sound Settings
    public SoundOption alertSound = SoundOption.EXPERIENCE_ORB;
    public float alertSoundVolume = 1.0F;
    public float alertSoundPitch = 1.0F;
    public boolean alertSoundEnabled = true;

    // X-Ray Settings
    public int xrayAlertThreshold = 4;
    public int xrayTimeWindowSeconds = 10;
    public SoundOption xrayAlertSound = SoundOption.EXPERIENCE_ORB;
    public boolean xrayAlertSoundEnabled = true;
    public boolean xrayAlertPing = true;

    // Report Settings
    public SoundOption reportAlertSound = SoundOption.EXPERIENCE_ORB;
    public boolean reportAlertSoundEnabled = true;
    public boolean reportAlertPing = true;

    // Overlay Settings
    public boolean autoOpenOverlayOnFlag = true;
    public boolean autoOpenPunishGuiOnFlag = false;
    public boolean instantPunishForSpam = false;

    // HUD Position & Size
    public int hudX = -1;
    public int hudY = -1;
    public int hudWidth = 250;
    public int hudHeight = 130;

    // Evidence Settings
    public boolean evidenceScreenshotEnabled = true;
    public String evidenceModeratorName = "";

    // ========== Customizable Action Commands ==========
    // Use {player} as placeholder for the target username
    // Commands are sent without the leading slash

    // Command #1 (Default: Alts)
    public String command1Label = "Check Alts";
    public String command1Command = "alts {player} true";

    // Command #2 (Default: Check Fly)
    public String command2Label = "Check Fly";
    public String command2Command = "checkfly {player}";

    // Command #3 (Default: Approve Report)
    public String command3Label = "Approve Report";
    public String command3Command = "approvereport {player}";

    /**
     * Creates a new ModConfig with default values.
     */
    public ModConfig() {
        initializeDefaults();
    }

    private void initializeDefaults() {
        this.alertSound = SoundOption.EXPERIENCE_ORB;
        this.alertSoundVolume = 1.0F;
        this.alertSoundPitch = 1.0F;
        this.alertSoundEnabled = true;
        this.autoOpenOverlayOnFlag = true;
        this.autoOpenPunishGuiOnFlag = false;
        this.instantPunishForSpam = false;
        this.evidenceScreenshotEnabled = true;
        this.xrayAlertSound = SoundOption.EXPERIENCE_ORB;
        this.xrayAlertSoundEnabled = true;
        this.reportAlertSound = SoundOption.EXPERIENCE_ORB;
        this.reportAlertSoundEnabled = true;
    }

    /**
     * Processes a command template by replacing {player} with the actual username.
     * 
     * @param template The command template with {player} placeholder
     * @param username The username to insert
     * @return The processed command
     */
    public static String processCommand(String template, String username) {
        return template.replace("{player}", username);
    }
}
