package sevengtz.autochatmod.service;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.AutoChatMod;
import sevengtz.autochatmod.config.ConfigManager;
import sevengtz.autochatmod.config.ModConfig;
import sevengtz.autochatmod.detection.*;
import sevengtz.autochatmod.integration.DiscordWebhook;
import sevengtz.autochatmod.model.FlagType;
import sevengtz.autochatmod.model.MessageEntry;
import sevengtz.autochatmod.model.PendingNickResolution;
import sevengtz.autochatmod.model.UsernameInfo;
import sevengtz.autochatmod.util.SoundUtils;
import sevengtz.autochatmod.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service for monitoring and processing chat messages.
 * Coordinates detection services and handles flagged message actions.
 */
public class ChatMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");

    // Detection services
    private final MessageParser messageParser;
    private final MessageFilter messageFilter;
    private final SpamDetector spamDetector;
    private final TermDetector termDetector;
    private final XRayDetector xrayDetector;
    private final ReportDetector reportDetector;
    private final DiscordWebhook discordWebhook;

    // Pending resolutions for nicknames
    private final Map<String, PendingNickResolution> pendingNickResolutions = new ConcurrentHashMap<>();
    private final Map<String, String> pendingDiscordNotifications = new ConcurrentHashMap<>();
    private final Set<String> flaggedMessages = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ChatMonitorService() {
        this.messageParser = new MessageParser();
        this.messageFilter = new MessageFilter(messageParser);
        this.spamDetector = new SpamDetector();
        this.termDetector = new TermDetector();
        this.xrayDetector = new XRayDetector(messageParser);
        this.reportDetector = new ReportDetector(messageParser);
        this.discordWebhook = new DiscordWebhook();
    }

    /**
     * Makes a chat message clickable to open action menu.
     * 
     * @param originalMessage The original message Text
     * @return The modified message with click events
     */
    public Text makeMessageClickable(Text originalMessage) {
        if (originalMessage == null)
            return originalMessage;

        String plainText = originalMessage.getString();
        String cleanText = StringUtils.stripColorCodes(plainText);

        UsernameInfo userInfo = messageParser.extractUsernameInfo(cleanText);
        if (userInfo == null) {
            // Check for special patterns
            return handleSpecialPatternClickable(originalMessage, cleanText);
        }

        if (userInfo.isNick()) {
            return createNicknameClickable(originalMessage, cleanText, userInfo);
        } else {
            return createUsernameClickable(originalMessage, userInfo.username());
        }
    }

    private Text handleSpecialPatternClickable(Text originalMessage, String cleanText) {
        // Check X-Ray pattern
        String[] xrayInfo = messageParser.parseXRayMessage(cleanText);
        if (xrayInfo != null) {
            return createUsernameClickable(originalMessage, xrayInfo[0]);
        }

        // Check Report pattern - use reportee
        String[] reportInfo = messageParser.parseReportMessage(cleanText);
        if (reportInfo != null) {
            String reportee = reportInfo[1];
            ClickEvent click = new ClickEvent.RunCommand("/autochatmod action " + reportee);
            HoverEvent hover = new HoverEvent.ShowText(
                    Text.literal("Click for actions on Reportee: " + reportee));
            return originalMessage.copy()
                    .setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
        }

        return originalMessage;
    }

    private Text createNicknameClickable(Text originalMessage, String cleanText, UsernameInfo userInfo) {
        String nickKey = userInfo.username().toLowerCase();
        pendingNickResolutions.put(nickKey, PendingNickResolution.forAction(cleanText, userInfo.username()));

        ClickEvent click = new ClickEvent.RunCommand("/realname " + userInfo.username());
        HoverEvent hover = new HoverEvent.ShowText(
                Text.literal("Click to resolve and open actions for " + userInfo.username()));
        return originalMessage.copy()
                .setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
    }

    private Text createUsernameClickable(Text originalMessage, String username) {
        ClickEvent click = new ClickEvent.RunCommand("/autochatmod action " + username);
        HoverEvent hover = new HoverEvent.ShowText(Text.literal("Click for actions on " + username));
        return originalMessage.copy()
                .setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
    }

    /**
     * Processes an incoming chat message for moderation.
     * Only the message CONTENT is checked for flagged terms/phrases.
     * Messages where we cannot extract a username are skipped.
     * 
     * @param message The message to process
     */
    public void processMessage(String message) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.enabled)
            return;

        String cleanMessage = StringUtils.stripColorCodes(message);

        // Priority: Check specific types (X-Ray / Report) BEFORE ignore check
        // These have their own username extraction logic
        if (handleXRay(cleanMessage, config))
            return;
        if (handleReport(cleanMessage, config))
            return;

        if (messageFilter.shouldIgnoreMessage(message, config)) {
            LOGGER.debug("[AutoChatMod]: Message ignored: {}", message);
            return;
        }

        if (cleanMessage.trim().isEmpty())
            return;

        // Handle realname responses
        if (handleRealnameResponse(cleanMessage))
            return;

        // IMPORTANT: Extract username info FIRST
        // If we can't extract a username, skip this message entirely
        UsernameInfo userInfo = messageParser.extractUsernameInfo(cleanMessage);
        if (userInfo == null) {
            LOGGER.debug("[AutoChatMod]: Skipping message - could not extract username: {}", cleanMessage);
            return;
        }

        // Get ONLY the message content for detection (not the full chat line)
        String messageContent = userInfo.messageContent();
        if (messageContent == null || messageContent.trim().isEmpty()) {
            LOGGER.debug("[AutoChatMod]: Skipping message - empty content for user {}", userInfo.username());
            return;
        }

        // Run detection checks on MESSAGE CONTENT ONLY
        boolean isSpam = config.spamDetectionEnabled && checkSpam(messageContent, userInfo, config);

        if (!isSpam) {
            if (config.phraseDetectionEnabled) {
                var phraseResult = termDetector.checkFlaggedPhrases(messageContent, config);
                if (phraseResult.isDetected()) {
                    handleFlaggedMessageWithUser(cleanMessage, userInfo, false);
                    return;
                }
            }

            if (config.termDetectionEnabled) {
                var termResult = termDetector.checkFlaggedTerms(messageContent, config);
                if (termResult.isDetected()) {
                    handleFlaggedMessageWithUser(cleanMessage, userInfo, false);
                    return;
                }
            }
        } else {
            handleFlaggedMessageWithUser(cleanMessage, userInfo, true);
        }
    }

    private boolean checkSpam(String messageContent, UsernameInfo userInfo, ModConfig config) {
        var spamResult = spamDetector.checkForSpam(messageContent, userInfo.username(), config);
        if (spamResult.isSpam()) {
            if (!userInfo.isNick()) {
                handleDirectSpamWithUser(messageContent, userInfo, spamResult.similarMessages(), config);
            }
            return true;
        }
        return false;
    }

    /**
     * Handles a flagged message when we already have the username info.
     */
    private void handleFlaggedMessageWithUser(String originalMessage, UsernameInfo userInfo, boolean isSpam) {
        if (userInfo.isNick()) {
            handleNicknameFlagged(originalMessage, userInfo, isSpam);
        } else {
            executeActions(userInfo.username(), originalMessage, isSpam);
        }
    }

    /**
     * Handles direct spam when we already have the username info.
     */
    private void handleDirectSpamWithUser(String messageContent, UsernameInfo userInfo,
            List<MessageEntry> similarMessages, ModConfig config) {
        for (MessageEntry entry : similarMessages) {
            flaggedMessages.add(entry.message());
        }
        flaggedMessages.add(messageContent);

        if (config.enableDiscordPing) {
            List<String> history = similarMessages.stream().map(MessageEntry::message).toList();
            discordWebhook.sendSpamAlert(userInfo.username(), messageContent, history);
        }

        spamDetector.clearHistory(userInfo.username());
        SoundUtils.playAlertSound(config);
    }

    private boolean handleXRay(String cleanMessage, ModConfig config) {
        var result = xrayDetector.checkForXRay(cleanMessage, config);
        if (!result.isAlert() && result.username() == null) {
            return false; // Not an X-Ray message at all
        }

        if (!isValidTarget(result.username())) {
            return true; // Was X-Ray but invalid target
        }

        if (result.isAlert()) {
            if (config.xrayAlertSoundEnabled) {
                SoundUtils.playSound(config.xrayAlertSound, config.alertSoundVolume, config.alertSoundPitch);
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (config.autoOpenOverlayOnFlag) {
                client.execute(() -> AutoChatMod.getActionMenu().show(result.username(), FlagType.XRAY));
            }

            if (config.enableDiscordPing && config.xrayAlertPing) {
                discordWebhook.sendXRayAlert(result.username(), result.count(), result.oreType());
            }
        }

        return true;
    }

    private boolean handleReport(String cleanMessage, ModConfig config) {
        var result = reportDetector.checkForReport(cleanMessage);
        if (!result.isReport()) {
            return false;
        }

        if (!isValidTarget(result.reportee(), false)) {
            return true;
        }

        if (config.reportAlertSoundEnabled) {
            SoundUtils.playSound(config.reportAlertSound, config.alertSoundVolume, config.alertSoundPitch);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (config.autoOpenOverlayOnFlag) {
            client.execute(() -> AutoChatMod.getActionMenu().show(result.reportee(), FlagType.REPORT));
        }

        if (config.enableDiscordPing && config.reportAlertPing) {
            discordWebhook.sendReportAlert(result.reporter(), result.reportee(), result.reason());
        }

        return true;
    }

    private boolean handleRealnameResponse(String message) {
        String[] realnameInfo = messageFilter.parseRealnameResponse(message);
        if (realnameInfo == null)
            return false;

        String realUsername = realnameInfo[0];
        String originalNick = realnameInfo[1];
        String nickKey = originalNick.toLowerCase();

        PendingNickResolution pending = pendingNickResolutions.remove(nickKey);
        LOGGER.info("[AutoChatMod]: Resolved {} -> {}. Opening action menu.", originalNick, realUsername);

        // Always open the overlay
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.execute(() -> client.player.networkHandler.sendChatCommand("autochatmod action " + realUsername));
        }

        // Handle pending actions
        if (pending != null && !pending.openActionOnResolve()) {
            executeActions(realUsername, pending.originalMessage(), pending.isSpam());
        }

        // Handle pending Discord notifications
        String pendingDiscordMessage = pendingDiscordNotifications.remove(nickKey);
        if (pendingDiscordMessage != null) {
            boolean sentInActions = pending != null && !pending.openActionOnResolve() && !pending.isSpam();
            if (!sentInActions) {
                String resolvedMessage = pendingDiscordMessage.replace(originalNick, realUsername);
                discordWebhook.sendMessage(resolvedMessage);
            }
        }

        return true;
    }

    private void handleNicknameFlagged(String message, UsernameInfo userInfo, boolean isSpam) {
        String nick = userInfo.username();
        LOGGER.info("[AutoChatMod]: Flagged message from nick '{}'. Will resolve on user click.", nick);

        List<MessageEntry> similarMessages = isSpam
                ? spamDetector.collectSimilarMessages(message, userInfo.username(),
                        ConfigManager.getConfig().spamSimilarityThreshold)
                : null;

        pendingNickResolutions.put(nick.toLowerCase(),
                PendingNickResolution.forFlagged(message, nick, isSpam, similarMessages));

        ModConfig config = ConfigManager.getConfig();
        if (config.enableDiscordPing) {
            String alertType = isSpam ? "Spam detected" : "Flagged message";
            String discordMessage = DiscordWebhook.formatAlert(alertType, nick, userInfo.messageContent());
            pendingDiscordNotifications.put(nick.toLowerCase(), discordMessage);

            // Auto-resolve nickname
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.execute(() -> client.player.networkHandler.sendChatCommand("realname " + nick));
            }
        }
    }

    private void executeActions(String username, String originalMessage, boolean isSpam) {
        ModConfig config = ConfigManager.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        LOGGER.info("[AutoChatMod]: Executing actions for user '{}' on message: {}", username, originalMessage);

        if (config.enableDiscordPing && !isSpam) {
            String content = extractCleanMessage(originalMessage);
            discordWebhook.sendFlaggedAlert(username, content);
        }

        displayFlaggedMessage(username, originalMessage, isSpam);
        SoundUtils.playAlertSound(config);

        if (config.autoOpenOverlayOnFlag) {
            FlagType type = isSpam ? FlagType.SPAM : FlagType.FLAGGED_PHRASE;
            client.execute(() -> AutoChatMod.getActionMenu().show(username, type));

            boolean shouldAutoPunish = config.autoOpenPunishGuiOnFlag &&
                    (!isSpam || !config.instantPunishForSpam);
            if (shouldAutoPunish && client.player != null) {
                client.execute(() -> client.player.networkHandler.sendChatCommand("punish " + username));
            }
        }
    }

    private void displayFlaggedMessage(String username, String originalMessage, boolean isSpam) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        String cleanMessage = StringUtils.stripColorCodes(originalMessage);

        // Premium styled prefix with gradient-like effect
        // Format: ⚠ [SPAM] or 🚨 [FLAGGED]
        String icon = isSpam ? "⚠" : "🚨";
        String label = isSpam ? "SPAM" : "FLAGGED";

        MutableText prefixIcon = Text.literal(icon + " ")
                .setStyle(Style.EMPTY.withColor(isSpam ? 0xFFAA00 : 0xFF5555));

        MutableText prefixBracketOpen = Text.literal("[")
                .setStyle(Style.EMPTY.withColor(0x555555));

        MutableText prefixLabel = Text.literal(label)
                .setStyle(Style.EMPTY.withColor(isSpam ? 0xFFAA00 : 0xFF5555).withBold(true));

        MutableText prefixBracketClose = Text.literal("] ")
                .setStyle(Style.EMPTY.withColor(0x555555));

        MutableText prefix = prefixIcon.append(prefixBracketOpen).append(prefixLabel).append(prefixBracketClose);

        // Create clickable username
        MutableText usernameText = Text.literal(username)
                .setStyle(Style.EMPTY
                        .withColor(0x55FFFF)
                        .withBold(true)
                        .withUnderline(true)
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("▶ Click to open actions for ")
                                        .setStyle(Style.EMPTY.withColor(0xAAAAAA))
                                        .append(Text.literal(username)
                                                .setStyle(Style.EMPTY.withColor(0x55FFFF).withBold(true)))))
                        .withClickEvent(new ClickEvent.RunCommand("/autochatmod action " + username)));

        // Extract and format message content
        UsernameInfo userInfo = messageParser.extractUsernameInfo(cleanMessage);
        String content = userInfo != null ? userInfo.messageContent() : cleanMessage;

        MutableText contentText = Text.literal(": " + content)
                .setStyle(Style.EMPTY.withColor(0xFFFFFF));

        MutableText fullMessage = prefix.append(usernameText).append(contentText);

        client.execute(() -> client.player.sendMessage(fullMessage, false));
    }

    private boolean isValidTarget(String username) {
        return isValidTarget(username, true);
    }

    private boolean isValidTarget(String username, boolean requireOnline) {
        if (username == null || username.isEmpty())
            return false;

        ModConfig config = ConfigManager.getConfig();
        for (String ignored : config.ignoredSystemUsernames) {
            if (ignored.equalsIgnoreCase(username)) {
                LOGGER.debug("[ModAssist]: Ignoring system username (Config): {}", username);
                return false;
            }
        }

        if (!requireOnline)
            return true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            boolean isInTabList = client.getNetworkHandler().getPlayerList().stream()
                    .anyMatch(entry -> entry.getProfile().name().equalsIgnoreCase(username));

            if (!isInTabList) {
                LOGGER.debug("[ModAssist]: Ignoring target {} - Not found in Player List", username);
            }
            return isInTabList;
        }
        return false;
    }

    private String extractCleanMessage(String fullMessage) {
        String cleanMessage = StringUtils.stripColorCodes(fullMessage);
        UsernameInfo userInfo = messageParser.extractUsernameInfo(cleanMessage);
        if (userInfo != null) {
            return userInfo.username() + ": " + userInfo.messageContent();
        }
        return cleanMessage;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
}
