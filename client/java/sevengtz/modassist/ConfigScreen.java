//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sevengtz.autochatmod;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen {
    public static Screen create(Screen parent) {
        ConfigManager.Config config = ConfigManager.getConfig();
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Text.literal("AutoChatMod Configuration"))
                .setSavingRunnable(() -> ConfigManager.saveConfig());
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enabled"), config.enabled).setDefaultValue(true)
                .setTooltip(new Text[] { Text.literal("Enable/disable the entire mod") })
                .setSaveConsumer((newValue) -> config.enabled = newValue).build());
        general.addEntry(
                entryBuilder.startStrField(Text.literal("Discord Webhook URL"), config.webhookUrl).setDefaultValue("")
                        .setTooltip(new Text[] { Text.literal("Your Discord webhook URL for notifications") })
                        .setSaveConsumer((newValue) -> config.webhookUrl = newValue).build());
        general.addEntry(entryBuilder.startStrField(Text.literal("User Mention ID"), config.userMentionId)
                .setDefaultValue("").setTooltip(new Text[] { Text.literal("Discord user ID to mention in alerts") })
                .setSaveConsumer((newValue) -> config.userMentionId = newValue).build());
        general.addEntry(
                entryBuilder.startBooleanToggle(Text.literal("Ping on Discord Alert"), config.enableDiscordPing)
                        .setDefaultValue(true).setTooltip(new Text[] { Text.literal("Mention you in Discord alerts") })
                        .setSaveConsumer((newValue) -> config.enableDiscordPing = newValue).build());
        ConfigCategory detection = builder.getOrCreateCategory(Text.literal("Detection"));
        detection.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Auto-Open Overlay on Flag"), config.autoOpenOverlayOnFlag)
                .setDefaultValue(true)
                .setTooltip(
                        new Text[] { Text.literal("Automatically open the action overlay when a message is flagged.") })
                .setSaveConsumer((newValue) -> config.autoOpenOverlayOnFlag = newValue).build());
        detection.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Auto-Open Punish GUI on Flag"), config.autoOpenPunishGuiOnFlag)
                .setDefaultValue(false)
                .setTooltip(new Text[] { Text.literal("Automatically run /punish when a message is flagged."),
                        Text.literal("Requires 'Auto-Open Overlay' to be on.") })
                .setSaveConsumer((newValue) -> config.autoOpenPunishGuiOnFlag = newValue).build());
        detection.addEntry(
                entryBuilder.startBooleanToggle(Text.literal("Instant Punish for Spam"), config.instantPunishForSpam)
                        .setDefaultValue(false)
                        .setTooltip(new Text[] { Text.literal(
                                "If enabled, pressing the Punish keybind on a spammer will run /punish <user> i:1."),
                                Text.literal("The overlay will NOT auto-open for spammers if this is on.") })
                        .setSaveConsumer((newValue) -> config.instantPunishForSpam = newValue).build());
        detection.addEntry(entryBuilder.startBooleanToggle(Text.literal("Spam Detection"), config.spamDetectionEnabled)
                .setDefaultValue(true).setSaveConsumer((newValue) -> config.spamDetectionEnabled = newValue).build());
        detection.addEntry(entryBuilder.startBooleanToggle(Text.literal("Term Detection"), config.termDetectionEnabled)
                .setDefaultValue(true).setSaveConsumer((newValue) -> config.termDetectionEnabled = newValue).build());
        detection.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Phrase Detection"), config.phraseDetectionEnabled)
                .setDefaultValue(true).setSaveConsumer((newValue) -> config.phraseDetectionEnabled = newValue).build());
        detection.addEntry(entryBuilder
                .startDoubleField(Text.literal("Similarity Threshold"), config.similarityThreshold).setDefaultValue(0.8)
                .setMin((double) 0.0F).setMax((double) 1.0F)
                .setTooltip(new Text[] { Text.literal("How similar words need to be to flagged terms (0.0-1.0)") })
                .setSaveConsumer((newValue) -> config.similarityThreshold = newValue).build());
        ConfigCategory spam = builder.getOrCreateCategory(Text.literal("Spam Detection"));
        spam.addEntry(
                entryBuilder.startDoubleField(Text.literal("Spam Similarity Threshold"), config.spamSimilarityThreshold)
                        .setDefaultValue(0.9).setMin((double) 0.0F).setMax((double) 1.0F)
                        .setTooltip(new Text[] { Text.literal("How similar messages need to be to count as spam") })
                        .setSaveConsumer((newValue) -> config.spamSimilarityThreshold = newValue).build());
        spam.addEntry(entryBuilder.startIntField(Text.literal("Spam Message Count"), config.spamMessageCount)
                .setDefaultValue(3).setMin(2).setMax(10)
                .setTooltip(new Text[] { Text.literal("Number of similar messages to trigger spam detection") })
                .setSaveConsumer((newValue) -> config.spamMessageCount = newValue).build());
        spam.addEntry(
                entryBuilder.startIntField(Text.literal("Spam Time Window (seconds)"), config.spamTimeWindowSeconds)
                        .setDefaultValue(15).setMin(5).setMax(300)
                        .setTooltip(new Text[] { Text.literal("Time window for spam detection") })
                        .setSaveConsumer((newValue) -> config.spamTimeWindowSeconds = newValue).build());
        spam.addEntry(entryBuilder.startStrList(Text.literal("Spam Whitelist Prefixes"), config.spamWhitelistPrefixes)
                .setDefaultValue(ConfigManager.getDefaultSpamWhitelistPrefixes())
                .setTooltip(new Text[] {
                        Text.literal("Messages starting with these prefixes won't trigger spam detection") })
                .setSaveConsumer((newValue) -> config.spamWhitelistPrefixes = newValue).build());
        ConfigCategory terms = builder.getOrCreateCategory(Text.literal("Flagged Terms"));
        terms.addEntry(entryBuilder.startStrList(Text.literal("Flagged Terms"), config.flaggedTerms)
                .setDefaultValue(ConfigManager.getDefaultFlaggedTerms())
                .setTooltip(new Text[] { Text.literal("Words that will trigger alerts") })
                .setSaveConsumer((newValue) -> config.flaggedTerms = newValue).build());
        terms.addEntry(entryBuilder.startStrList(Text.literal("Whitelisted Terms"), config.whitelistedTerms)
                .setDefaultValue(ConfigManager.getDefaultWhitelistedTerms())
                .setTooltip(
                        new Text[] { Text.literal("Words that won't trigger alerts even if similar to flagged terms") })
                .setSaveConsumer((newValue) -> config.whitelistedTerms = newValue).build());
        ConfigCategory phrases = builder.getOrCreateCategory(Text.literal("Flagged Phrases"));
        phrases.addEntry(entryBuilder.startStrList(Text.literal("Flagged Phrases"), config.flaggedPhrases)
                .setDefaultValue(ConfigManager.getDefaultFlaggedPhrases())
                .setTooltip(new Text[] { Text.literal("Phrases that will trigger alerts") })
                .setSaveConsumer((newValue) -> config.flaggedPhrases = newValue).build());
        phrases.addEntry(entryBuilder.startStrList(Text.literal("Whitelisted Phrases"), config.whitelistedPhrases)
                .setDefaultValue(ConfigManager.getDefaultWhitelistedPhrases())
                .setTooltip(new Text[] { Text.literal("Phrases that won't trigger alerts") })
                .setSaveConsumer((newValue) -> config.whitelistedPhrases = newValue).build());
        ConfigCategory xrayCat = builder.getOrCreateCategory(Text.literal("X-Ray Detection"));
        xrayCat.addEntry(entryBuilder.startIntField(Text.literal("X-Ray Log Threshold"), config.xrayAlertThreshold)
                .setDefaultValue(4)
                .setMin(1)
                .setMax(20)
                .setTooltip(Text.literal("How many separate X-Ray log messages must appear to trigger an alert."))
                .setSaveConsumer(newValue -> config.xrayAlertThreshold = newValue)
                .build());

        xrayCat.addEntry(entryBuilder.startIntField(Text.literal("X-Ray Time Window (s)"), config.xrayTimeWindowSeconds)
                .setDefaultValue(10)
                .setMin(1)
                .setMax(60)
                .setTooltip(Text.literal("Time window to count the log messages."))
                .setSaveConsumer(newValue -> config.xrayTimeWindowSeconds = newValue)
                .build());
        ConfigCategory sound = builder.getOrCreateCategory(Text.literal("Sounds"));
        sound.addEntry(
                entryBuilder.startEnumSelector(Text.literal("Chat Alert Sound"), SoundOption.class, config.alertSound)
                        .setDefaultValue(SoundOption.EXPERIENCE_ORB)
                        .setTooltip(new Text[] { Text.literal("The sound to play when a message is flagged.") })
                        .setEnumNameProvider((option) -> ((SoundOption) option).toText())
                        .setSaveConsumer((newValue) -> config.alertSound = newValue).build());
        sound.addEntry(entryBuilder
                .startEnumSelector(Text.literal("X-Ray Alert Sound"), SoundOption.class, config.xrayAlertSound)
                .setDefaultValue(SoundOption.EXPERIENCE_ORB)
                .setTooltip(new Text[] { Text.literal("The sound to play when an X-Ray alert is triggered.") })
                .setEnumNameProvider((option) -> ((SoundOption) option).toText())
                .setSaveConsumer((newValue) -> config.xrayAlertSound = newValue).build());
        sound.addEntry(entryBuilder
                .startEnumSelector(Text.literal("Report Alert Sound"), SoundOption.class, config.reportAlertSound)
                .setDefaultValue(SoundOption.EXPERIENCE_ORB)
                .setTooltip(new Text[] { Text.literal("The sound to play when a Report is detected.") })
                .setEnumNameProvider((option) -> ((SoundOption) option).toText())
                .setSaveConsumer((newValue) -> config.reportAlertSound = newValue).build());

        xrayCat.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("X-Ray Discord Ping"), config.xrayAlertPing)
                .setDefaultValue(true)
                .setTooltip(new Text[] { Text.literal("Enable/disable Discord pings for X-Ray alerts") })
                .setSaveConsumer((newValue) -> config.xrayAlertPing = newValue).build());

        ConfigCategory reportCat = builder.getOrCreateCategory(Text.literal("Report Detection"));
        reportCat.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Report Discord Ping"), config.reportAlertPing)
                .setDefaultValue(true)
                .setTooltip(new Text[] { Text.literal("Enable/disable Discord pings for Report alerts") })
                .setSaveConsumer((newValue) -> config.reportAlertPing = newValue).build());
        sound.addEntry(entryBuilder.startFloatField(Text.literal("Alert Sound Volume"), config.alertSoundVolume)
                .setDefaultValue(1.0F).setMin(0.0F).setMax(2.0F)
                .setTooltip(new Text[] { Text.literal("The volume of the alert sound (0.0 to 2.0)") })
                .setSaveConsumer((newValue) -> config.alertSoundVolume = newValue).build());
        sound.addEntry(entryBuilder.startFloatField(Text.literal("Alert Sound Pitch"), config.alertSoundPitch)
                .setDefaultValue(1.0F).setMin(0.1F).setMax(2.0F)
                .setTooltip(new Text[] { Text.literal("The pitch of the alert sound (0.1 to 2.0)") })
                .setSaveConsumer((newValue) -> config.alertSoundPitch = newValue).build());
        sound.addEntry(entryBuilder.startBooleanToggle(Text.literal("Chat Sound Enabled"), config.alertSoundEnabled)
                .setDefaultValue(true)
                .setTooltip(new Text[] { Text.literal("Enable/disable the standard chat alert sound") })
                .setSaveConsumer((newValue) -> config.alertSoundEnabled = newValue).build());
        sound.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("X-Ray Sound Enabled"), config.xrayAlertSoundEnabled)
                .setDefaultValue(true).setTooltip(new Text[] { Text.literal("Enable/disable the X-Ray alert sound") })
                .setSaveConsumer((newValue) -> config.xrayAlertSoundEnabled = newValue).build());
        sound.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Report Sound Enabled"), config.reportAlertSoundEnabled)
                .setDefaultValue(true).setTooltip(new Text[] { Text.literal("Enable/disable the Report alert sound") })
                .setSaveConsumer((newValue) -> config.reportAlertSoundEnabled = newValue).build());

        ConfigCategory evidence = builder.getOrCreateCategory(Text.literal("Evidence"));
        evidence.addEntry(entryBuilder
                .startBooleanToggle(Text.literal("Enable Evidence Screenshots"), config.evidenceScreenshotEnabled)
                .setDefaultValue(true)
                .setTooltip(new Text[] { Text.literal("Automatically take a screenshot when you mute a player.") })
                .setSaveConsumer((newValue) -> config.evidenceScreenshotEnabled = newValue).build());

        return builder.build();
    }
}
