package sevengtz.autochatmod.ui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import sevengtz.autochatmod.config.ConfigManager;
import sevengtz.autochatmod.config.ModConfig;
import sevengtz.autochatmod.config.SoundOption;

/**
 * Creates the mod configuration screen using Cloth Config API.
 * Organizes settings into logical categories for easy navigation.
 */
public class ConfigScreen {

        private ConfigScreen() {
                // Utility class - prevent instantiation
        }

        /**
         * Creates the configuration screen.
         * 
         * @param parent The parent screen to return to
         * @return The created configuration screen
         */
        public static Screen create(Screen parent) {
                ModConfig config = ConfigManager.getConfig();

                ConfigBuilder builder = ConfigBuilder.create()
                                .setParentScreen(parent)
                                .setTitle(Text.literal("ModAssist Configuration"))
                                .setSavingRunnable(ConfigManager::saveConfig);

                ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                // Build all categories
                buildGeneralCategory(builder, entryBuilder, config);
                buildDetectionCategory(builder, entryBuilder, config);
                buildActionsCategory(builder, entryBuilder, config);
                buildSpamCategory(builder, entryBuilder, config);
                buildTermsCategory(builder, entryBuilder, config);
                buildPhrasesCategory(builder, entryBuilder, config);
                buildXRayCategory(builder, entryBuilder, config);
                buildSoundCategory(builder, entryBuilder, config);
                buildEvidenceCategory(builder, entryBuilder, config);

                return builder.build();
        }

        private static void buildGeneralCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

                general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enabled"), config.enabled)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("Enable/disable the entire mod"))
                                .setSaveConsumer(v -> config.enabled = v)
                                .build());

                general.addEntry(entryBuilder
                                .startStrField(Text.literal("Discord Webhook URL (Detailed)"), config.webhookUrl)
                                .setDefaultValue("")
                                .setTooltip(Text.literal("Standard webhook or Bot Base URL"))
                                .setSaveConsumer(v -> config.webhookUrl = v)
                                .build());

                general.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Use Custom Bot API"), config.useCustomBot)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal(
                                                "If true, sends JSON payloads to the configured Bot URL instead of Discord messages."))
                                .setSaveConsumer(v -> config.useCustomBot = v)
                                .build());

                general.addEntry(entryBuilder.startStrField(Text.literal("User Mention ID"), config.userMentionId)
                                .setDefaultValue("")
                                .setTooltip(Text.literal("Discord user ID to mention in alerts"))
                                .setSaveConsumer(v -> config.userMentionId = v)
                                .build());

                general.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Ping on Discord Alert"), config.enableDiscordPing)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("Mention you in Discord alerts"))
                                .setSaveConsumer(v -> config.enableDiscordPing = v)
                                .build());
        }

        private static void buildDetectionCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory detection = builder.getOrCreateCategory(Text.literal("Detection"));

                detection.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Auto-Open Overlay on Flag"),
                                                config.autoOpenOverlayOnFlag)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal(
                                                "Automatically open the action overlay when a message is flagged."))
                                .setSaveConsumer(v -> config.autoOpenOverlayOnFlag = v)
                                .build());

                detection.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Auto-Open Punish GUI on Flag"),
                                                config.autoOpenPunishGuiOnFlag)
                                .setDefaultValue(false)
                                .setTooltip(Text.literal("Automatically run /punish when a message is flagged."),
                                                Text.literal("Requires 'Auto-Open Overlay' to be on."))
                                .setSaveConsumer(v -> config.autoOpenPunishGuiOnFlag = v)
                                .build());

                detection.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Instant Punish for Spam"),
                                                config.instantPunishForSpam)
                                .setDefaultValue(false)
                                .setTooltip(
                                                Text.literal(
                                                                "If enabled, pressing the Punish keybind on a spammer will run /punish <user> i:1."),
                                                Text.literal("The overlay will NOT auto-open for spammers if this is on."))
                                .setSaveConsumer(v -> config.instantPunishForSpam = v)
                                .build());

                detection.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Spam Detection"), config.spamDetectionEnabled)
                                .setDefaultValue(true)
                                .setSaveConsumer(v -> config.spamDetectionEnabled = v)
                                .build());

                detection.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Term Detection"), config.termDetectionEnabled)
                                .setDefaultValue(true)
                                .setSaveConsumer(v -> config.termDetectionEnabled = v)
                                .build());

                detection.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Phrase Detection"), config.phraseDetectionEnabled)
                                .setDefaultValue(true)
                                .setSaveConsumer(v -> config.phraseDetectionEnabled = v)
                                .build());

                detection.addEntry(entryBuilder
                                .startDoubleField(Text.literal("Similarity Threshold"), config.similarityThreshold)
                                .setDefaultValue(0.8)
                                .setMin(0.0).setMax(1.0)
                                .setTooltip(Text.literal("How similar words need to be to flagged terms (0.0-1.0)"))
                                .setSaveConsumer(v -> config.similarityThreshold = v)
                                .build());
        }

        private static void buildActionsCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory actions = builder.getOrCreateCategory(Text.literal("⌨ Action Commands"));

                // Header note
                actions.addEntry(entryBuilder
                                .startTextDescription(Text
                                                .literal("§7Use §e{player}§7 as placeholder for the target username."))
                                .build());

                // Command #1
                actions.addEntry(entryBuilder
                                .startStrField(Text.literal("Command #1 Label"), config.command1Label)
                                .setDefaultValue("Check Alts")
                                .setTooltip(Text.literal("Display name for Command #1"))
                                .setSaveConsumer(v -> config.command1Label = v)
                                .build());

                actions.addEntry(entryBuilder
                                .startStrField(Text.literal("Command #1 Command"), config.command1Command)
                                .setDefaultValue("alts {player} true")
                                .setTooltip(Text.literal("Command to run for Command #1"))
                                .setSaveConsumer(v -> config.command1Command = v)
                                .build());

                // Command #2
                actions.addEntry(entryBuilder
                                .startStrField(Text.literal("Command #2 Label"), config.command2Label)
                                .setDefaultValue("Check Fly")
                                .setTooltip(Text.literal("Display name for Command #2"))
                                .setSaveConsumer(v -> config.command2Label = v)
                                .build());

                actions.addEntry(entryBuilder
                                .startStrField(Text.literal("Command #2 Command"), config.command2Command)
                                .setDefaultValue("checkfly {player}")
                                .setTooltip(Text.literal("Command to run for Command #2"))
                                .setSaveConsumer(v -> config.command2Command = v)
                                .build());

                // Command #3
                actions.addEntry(entryBuilder
                                .startStrField(Text.literal("Command #3 Label"), config.command3Label)
                                .setDefaultValue("Approve Report")
                                .setTooltip(Text.literal("Display name for Command #3"))
                                .setSaveConsumer(v -> config.command3Label = v)
                                .build());

                actions.addEntry(entryBuilder
                                .startStrField(Text.literal("Command #3 Command"), config.command3Command)
                                .setDefaultValue("approvereport {player}")
                                .setTooltip(Text.literal("Command to run for Command #3"))
                                .setSaveConsumer(v -> config.command3Command = v)
                                .build());
        }

        private static void buildSpamCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory spam = builder.getOrCreateCategory(Text.literal("Spam Detection"));

                spam.addEntry(entryBuilder
                                .startDoubleField(Text.literal("Spam Similarity Threshold"),
                                                config.spamSimilarityThreshold)
                                .setDefaultValue(0.9)
                                .setMin(0.0).setMax(1.0)
                                .setTooltip(Text.literal("How similar messages need to be to count as spam"))
                                .setSaveConsumer(v -> config.spamSimilarityThreshold = v)
                                .build());

                spam.addEntry(entryBuilder
                                .startIntField(Text.literal("Spam Message Count"), config.spamMessageCount)
                                .setDefaultValue(3)
                                .setMin(2).setMax(10)
                                .setTooltip(Text.literal("Number of similar messages to trigger spam detection"))
                                .setSaveConsumer(v -> config.spamMessageCount = v)
                                .build());

                spam.addEntry(entryBuilder
                                .startIntField(Text.literal("Spam Time Window (seconds)"), config.spamTimeWindowSeconds)
                                .setDefaultValue(15)
                                .setMin(5).setMax(300)
                                .setTooltip(Text.literal("Time window for spam detection"))
                                .setSaveConsumer(v -> config.spamTimeWindowSeconds = v)
                                .build());

                spam.addEntry(entryBuilder
                                .startStrList(Text.literal("Spam Whitelist Prefixes"), config.spamWhitelistPrefixes)
                                .setDefaultValue(ConfigManager.getDefaultSpamWhitelistPrefixes())
                                .setTooltip(Text.literal(
                                                "Messages starting with these prefixes won't trigger spam detection"))
                                .setSaveConsumer(v -> config.spamWhitelistPrefixes = v)
                                .build());
        }

        private static void buildTermsCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory terms = builder.getOrCreateCategory(Text.literal("Flagged Terms"));

                terms.addEntry(entryBuilder
                                .startStrList(Text.literal("Flagged Terms"), config.flaggedTerms)
                                .setDefaultValue(ConfigManager.getDefaultFlaggedTerms())
                                .setTooltip(Text.literal("Words that will trigger alerts"))
                                .setSaveConsumer(v -> config.flaggedTerms = v)
                                .build());

                terms.addEntry(entryBuilder
                                .startStrList(Text.literal("Whitelisted Terms"), config.whitelistedTerms)
                                .setDefaultValue(ConfigManager.getDefaultWhitelistedTerms())
                                .setTooltip(Text.literal(
                                                "Words that won't trigger alerts even if similar to flagged terms"))
                                .setSaveConsumer(v -> config.whitelistedTerms = v)
                                .build());
        }

        private static void buildPhrasesCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory phrases = builder.getOrCreateCategory(Text.literal("Flagged Phrases"));

                phrases.addEntry(entryBuilder
                                .startStrList(Text.literal("Flagged Phrases"), config.flaggedPhrases)
                                .setDefaultValue(ConfigManager.getDefaultFlaggedPhrases())
                                .setTooltip(Text.literal("Phrases that will trigger alerts"))
                                .setSaveConsumer(v -> config.flaggedPhrases = v)
                                .build());

                phrases.addEntry(entryBuilder
                                .startStrList(Text.literal("Whitelisted Phrases"), config.whitelistedPhrases)
                                .setDefaultValue(ConfigManager.getDefaultWhitelistedPhrases())
                                .setTooltip(Text.literal("Phrases that won't trigger alerts"))
                                .setSaveConsumer(v -> config.whitelistedPhrases = v)
                                .build());
        }

        private static void buildXRayCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory xray = builder.getOrCreateCategory(Text.literal("X-Ray Detection"));

                xray.addEntry(entryBuilder
                                .startIntField(Text.literal("X-Ray Log Threshold"), config.xrayAlertThreshold)
                                .setDefaultValue(4)
                                .setMin(1).setMax(20)
                                .setTooltip(Text.literal(
                                                "How many separate X-Ray log messages must appear to trigger an alert."))
                                .setSaveConsumer(v -> config.xrayAlertThreshold = v)
                                .build());

                xray.addEntry(entryBuilder
                                .startIntField(Text.literal("X-Ray Time Window (s)"), config.xrayTimeWindowSeconds)
                                .setDefaultValue(10)
                                .setMin(1).setMax(60)
                                .setTooltip(Text.literal("Time window to count the log messages."))
                                .setSaveConsumer(v -> config.xrayTimeWindowSeconds = v)
                                .build());
        }

        private static void buildSoundCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory sound = builder.getOrCreateCategory(Text.literal("🔊 Sounds"));

                sound.addEntry(entryBuilder
                                .startEnumSelector(Text.literal("Chat Alert Sound"), SoundOption.class,
                                                config.alertSound)
                                .setDefaultValue(SoundOption.EXPERIENCE_ORB)
                                .setTooltip(Text.literal("The sound to play when a message is flagged."))
                                .setEnumNameProvider(option -> ((SoundOption) option).toText())
                                .setSaveConsumer(v -> config.alertSound = v)
                                .build());

                sound.addEntry(entryBuilder
                                .startEnumSelector(Text.literal("X-Ray Alert Sound"), SoundOption.class,
                                                config.xrayAlertSound)
                                .setDefaultValue(SoundOption.EXPERIENCE_ORB)
                                .setTooltip(Text.literal("The sound to play when an X-Ray alert is triggered."))
                                .setEnumNameProvider(option -> ((SoundOption) option).toText())
                                .setSaveConsumer(v -> config.xrayAlertSound = v)
                                .build());

                sound.addEntry(entryBuilder
                                .startEnumSelector(Text.literal("Report Alert Sound"), SoundOption.class,
                                                config.reportAlertSound)
                                .setDefaultValue(SoundOption.EXPERIENCE_ORB)
                                .setTooltip(Text.literal("The sound to play when a Report is detected."))
                                .setEnumNameProvider(option -> ((SoundOption) option).toText())
                                .setSaveConsumer(v -> config.reportAlertSound = v)
                                .build());

                sound.addEntry(entryBuilder
                                .startFloatField(Text.literal("Alert Sound Volume"), config.alertSoundVolume)
                                .setDefaultValue(1.0F)
                                .setMin(0.0F).setMax(2.0F)
                                .setTooltip(Text.literal("The volume of the alert sound (0.0 to 2.0)"))
                                .setSaveConsumer(v -> config.alertSoundVolume = v)
                                .build());

                sound.addEntry(entryBuilder
                                .startFloatField(Text.literal("Alert Sound Pitch"), config.alertSoundPitch)
                                .setDefaultValue(1.0F)
                                .setMin(0.1F).setMax(2.0F)
                                .setTooltip(Text.literal("The pitch of the alert sound (0.1 to 2.0)"))
                                .setSaveConsumer(v -> config.alertSoundPitch = v)
                                .build());

                sound.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Chat Sound Enabled"), config.alertSoundEnabled)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("Enable/disable the standard chat alert sound"))
                                .setSaveConsumer(v -> config.alertSoundEnabled = v)
                                .build());

                sound.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("X-Ray Sound Enabled"), config.xrayAlertSoundEnabled)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("Enable/disable the X-Ray alert sound"))
                                .setSaveConsumer(v -> config.xrayAlertSoundEnabled = v)
                                .build());

                sound.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Report Sound Enabled"),
                                                config.reportAlertSoundEnabled)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("Enable/disable the Report alert sound"))
                                .setSaveConsumer(v -> config.reportAlertSoundEnabled = v)
                                .build());
        }

        private static void buildEvidenceCategory(ConfigBuilder builder,
                        ConfigEntryBuilder entryBuilder, ModConfig config) {
                ConfigCategory evidence = builder.getOrCreateCategory(Text.literal("📷 Evidence"));

                evidence.addEntry(entryBuilder
                                .startBooleanToggle(Text.literal("Enable Evidence Screenshots"),
                                                config.evidenceScreenshotEnabled)
                                .setDefaultValue(true)
                                .setTooltip(Text.literal("Automatically take a screenshot when you mute a player."))
                                .setSaveConsumer(v -> config.evidenceScreenshotEnabled = v)
                                .build());
        }
}
