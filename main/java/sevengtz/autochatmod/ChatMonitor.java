package sevengtz.autochatmod;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

import com.mojang.authlib.GameProfile;

public class ChatMonitor {
    private final Queue<MessageEntry> messageHistory = new ConcurrentLinkedQueue<>();
    private final DiscordWebhook webhook;
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    private static final Pattern REALNAME_RESPONSE_PATTERN = Pattern
            .compile("^\\[\\*\\]\\s+(\\w{2,})\\s+is nicknamed as\\s+(\\w{2,})$");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)\u00A7.");
    private static final Pattern COREPROTECT_PATTERN = Pattern.compile("^\\d+\\.\\d{2}/[dmh]\\s+ago.*");
    private static final Pattern FILTERED_PRIVATE_MESSAGE_PATTERN = Pattern.compile(
            "^\\[S] \\[[^\\]]+] \\[Filtered] (?:\\[[^\\]]+] )?((?:\\* )?(\\w{2,})) \u00BB (?:\\[[^\\]]+] )?(?:\\* )?(\\w{2,}): (.*)$");
    private static final Pattern FILTERED_PATTERN = Pattern.compile(".*\\[Filtered]\\s+(\\w{2,})");
    private static final Pattern REPORT_PATTERN = Pattern
            .compile("^\\[S\\] \\[[^\\]]+\\] (\\w{2,}) reported (\\w{2,}) for (.*)$");
    private static final Pattern XRAY_PATTERN = Pattern
            .compile("^X-Ray\\s+\\u25B6\\s+(\\w{2,})\\s+found\\s+x(\\d+)\\s+(.+)$");
    private static final Pattern SERVER_FILTERED_PATTERN = Pattern.compile("^\\[S] \\[\\w+] \\[Filtered] (\\w{2,})");
    private static final Pattern PLAYERS_SLEEPING_PATTERN = Pattern.compile("^\\d+/\\d+\\s+players\\s+sleeping.*");

    private static final Pattern wordPattern = Pattern.compile("\\b\\w+\\b");

    private record PendingNickResolution(String originalMessage, String nick, boolean isSpam, long timestamp,
            List<MessageEntry> similarMessages,
            boolean openActionOnResolve) {
    }

    private final Map<String, PendingNickResolution> pendingNickResolutions = new ConcurrentHashMap<>();
    private final Map<String, String> pendingDiscordNotifications = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> xRayTracker = new ConcurrentHashMap<>();
    private final Set<String> flaggedMessages = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ChatMonitor() {
        this.webhook = new DiscordWebhook();
    }

    /**
     * Modify the vanilla message to make it clickable instead of sending a new
     * message
     */
    public Text makeMessageClickable(Text originalMessage) {
        if (originalMessage == null)
            return originalMessage;

        String plainText = originalMessage.getString();
        String cleanText = stripColorCodes(plainText);

        UsernameInfo userInfo = extractUsernameInfo(cleanText);
        if (userInfo == null) {
            // Check for special patterns if generic username extraction failed
            Matcher xrayMatcher = XRAY_PATTERN.matcher(cleanText);
            if (xrayMatcher.find()) {
                String username = xrayMatcher.group(1);
                ClickEvent click = new ClickEvent.RunCommand("/autochatmod action " + username);
                HoverEvent hover = new HoverEvent.ShowText(Text.literal("Click for actions on " + username));
                return originalMessage.copy()
                        .setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
            }

            Matcher reportMatcher = REPORT_PATTERN.matcher(cleanText);
            if (reportMatcher.find()) {
                // Group 1: Reporter, Group 2: Reportee
                String reportee = reportMatcher.group(2);
                ClickEvent click = new ClickEvent.RunCommand("/autochatmod action " + reportee);
                HoverEvent hover = new HoverEvent.ShowText(Text.literal("Click for actions on Reportee: " + reportee));
                return originalMessage.copy()
                        .setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
            }

            return originalMessage;
        }

        if (userInfo.isNick) {
            // Always create/update the pending resolution on every click
            // This ensures subsequent clicks work even after the previous resolution was
            // handled
            String nickKey = userInfo.username.toLowerCase();
            pendingNickResolutions.put(nickKey,
                    new PendingNickResolution(cleanText, userInfo.username, false, Instant.now().toEpochMilli(), null,
                            true));
            ClickEvent click = new ClickEvent.RunCommand("/realname " + userInfo.username);
            HoverEvent hover = new HoverEvent.ShowText(
                    Text.literal("Click to resolve and open actions for " + userInfo.username));
            return originalMessage.copy()
                    .setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
        } else {

            ClickEvent click = new ClickEvent.RunCommand("/autochatmod action " + userInfo.username);
            HoverEvent hover = new HoverEvent.ShowText(Text.literal("Click for actions on " + userInfo.username));
            return originalMessage.copy()
                    .setStyle(originalMessage.getStyle().withClickEvent(click).withHoverEvent(hover));
        }
    }

    /**
     * Container for username information
     */
    private static class UsernameInfo {
        final String username;
        final boolean isNick;
        final String messageContent;

        UsernameInfo(String username, boolean isNick, String messageContent) {
            this.username = username;
            this.isNick = isNick;
            this.messageContent = messageContent;
        }
    }

    private UsernameInfo extractUsernameInfo(String message) {

        Matcher privateMatcher = FILTERED_PRIVATE_MESSAGE_PATTERN.matcher(message);
        if (privateMatcher.find()) {
            String senderWithPossibleNick = privateMatcher.group(1);
            String actualSender = privateMatcher.group(2);
            String messageContent = privateMatcher.group(3);

            boolean senderIsNick = senderWithPossibleNick.startsWith("* ");

            LOGGER.debug("[AutoChatMod]: Extracted sender [{}] (nick: {}) from private message: {}",
                    actualSender, senderIsNick, message);
            return new UsernameInfo(actualSender, senderIsNick, messageContent);
        }

        Matcher serverFilteredMatcher = SERVER_FILTERED_PATTERN.matcher(message);
        if (serverFilteredMatcher.find()) {
            String username = serverFilteredMatcher.group(1);
            String messageContent = message.substring(serverFilteredMatcher.end()).trim();
            LOGGER.debug("[AutoChatMod]: Extracted username [{}] from server filtered pattern: {}", username, message);
            return new UsernameInfo(username, false, messageContent);
        }

        for (int i = 2; i < message.length(); i++) {
            if (message.charAt(i) == ':') {
                String beforeColon = message.substring(0, i);
                String afterColon = message.substring(i + 1).trim();

                if (afterColon.isEmpty()) {
                    continue;
                }

                UsernameInfo userInfo = parseUsernameFromBeforeColon(beforeColon, afterColon);
                if (userInfo != null) {
                    return userInfo;
                }
            }
        }

        Matcher reportMatcher = REPORT_PATTERN.matcher(message);
        if (reportMatcher.find()) {
            String username = reportMatcher.group(1);
            return new UsernameInfo(username, false, message);
        }

        Matcher filteredMatcher = FILTERED_PATTERN.matcher(message);
        if (filteredMatcher.find()) {
            String username = filteredMatcher.group(1);
            if (username.length() >= 2) {
                return new UsernameInfo(username, false, message);
            }
        }

        return null;
    }

    private UsernameInfo parseUsernameFromBeforeColon(String beforeColon, String messageContent) {
        String[] words = beforeColon.split("\\s+");
        if (words.length == 0) {
            return null;
        }

        String lastWord = words[words.length - 1].trim(); // This is the potential username

        // First, determine if it's a nickname by checking for a preceding '*'
        boolean isNick = false;
        if (words.length >= 2 && words[words.length - 2].equals("*")) {
            isNick = true;
        }

        boolean isValidUsername;
        if (isNick) {
            // Nicknames can be 1 or more characters long
            isValidUsername = lastWord.length() >= 1 && lastWord.matches("\\w+");
        } else {
            // Regular usernames must be 2 or more characters long
            isValidUsername = lastWord.length() >= 2 && lastWord.matches("\\w{2,}");
        }

        if (!isValidUsername) {
            return null; // If it doesn't meet the criteria, reject it.
        }

        // This is a good safeguard against parsing usernames inside ranks like [Admin]
        int lastWordStart = beforeColon.lastIndexOf(lastWord);
        if (lastWordStart > 0) {
            String beforeWord = beforeColon.substring(0, lastWordStart).trim();
            if (beforeWord.endsWith("[")) {
                int lastWordEnd = lastWordStart + lastWord.length();
                if (lastWordEnd < beforeColon.length()) {
                    String afterWord = beforeColon.substring(lastWordEnd).trim();
                    if (afterWord.startsWith("]")) {
                        return null;
                    }
                }
            }
        }

        // If we've made it this far, the username is valid.
        return new UsernameInfo(lastWord, isNick, messageContent);
    }

    public void processMessage(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        if (!config.enabled)
            return;

        String cleanMessage = stripColorCodes(message);
        // PRIORITY: Check specific types (XRAY / REPORT) BEFORE ignore check
        if (handleXRay(cleanMessage)) {
            return;
        }
        if (handleReport(cleanMessage)) {
            return;
        }

        if (shouldIgnoreMessage(message)) {
            LOGGER.debug("[AutoChatMod]: Message ignored: {}", message);
            return;
        }

        if (shouldIgnoreMessage(message)) {
            LOGGER.debug("[AutoChatMod]: Message ignored: {}", message);
            return;
        }

        // cleanMessage already defined above
        if (cleanMessage.trim().isEmpty())
            return;

        if (handleRealnameResponse(cleanMessage)) {
            return;
        }

        boolean isSpam = config.spamDetectionEnabled && checkForSpam(cleanMessage);
        boolean isFlaggedPhrase = !isSpam && config.phraseDetectionEnabled && checkFlaggedPhrases(cleanMessage);
        boolean isFlaggedTerm = !isSpam && !isFlaggedPhrase && config.termDetectionEnabled
                && checkFlaggedTerms(cleanMessage);

        if (isSpam || isFlaggedPhrase || isFlaggedTerm) {
            handleFlaggedMessage(cleanMessage, isSpam);
        }
    }

    private boolean handleXRay(String cleanMessage) {
        Matcher matcher = XRAY_PATTERN.matcher(cleanMessage);
        if (matcher.find()) {
            String username = matcher.group(1);
            if (!isValidTarget(username))
                return true;

            ConfigManager.Config config = ConfigManager.getConfig();
            long now = Instant.now().toEpochMilli();

            List<Long> history = xRayTracker.computeIfAbsent(username, k -> new ArrayList<>());
            history.add(now);

            long windowMillis = config.xrayTimeWindowSeconds * 1000L;
            history.removeIf(timestamp -> now - timestamp > windowMillis);

            if (history.size() >= config.xrayAlertThreshold) {
                LOGGER.info("[AutoChatMod]: X-Ray Alert triggered for {}", username);

                if (config.xrayAlertSoundEnabled) {
                    playSound(config.xrayAlertSound, config.alertSoundVolume, config.alertSoundPitch);
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (config.autoOpenOverlayOnFlag) {
                    client.execute(() -> AutoChatMod.ACTION_MENU.show(username, FlagType.XRAY));
                }
                // displaySpecialAlert("X-RAY", username, cleanMessage, Formatting.AQUA);

                if (config.enableDiscordPing && config.xrayAlertPing) {
                    webhook.sendMessage(String.format("`X-Ray Alert: %s found x%s %s`", username, matcher.group(2),
                            matcher.group(3)));
                }

                history.clear();
            }
            return true;
        }
        return false;
    }

    private boolean handleReport(String cleanMessage) {
        Matcher matcher = REPORT_PATTERN.matcher(cleanMessage);
        if (matcher.find()) {
            String reporter = matcher.group(1);
            String reportee = matcher.group(2);
            String reason = matcher.group(3);

            if (!isValidTarget(reportee, false))
                return true;

            ConfigManager.Config config = ConfigManager.getConfig();

            LOGGER.info("[AutoChatMod]: Report detected: {} reported {} for {}", reporter, reportee, reason);

            if (config.reportAlertSoundEnabled) {
                playSound(config.reportAlertSound, config.alertSoundVolume, config.alertSoundPitch);
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (config.autoOpenOverlayOnFlag) {
                client.execute(() -> AutoChatMod.ACTION_MENU.show(reportee, FlagType.REPORT));
            }
            // displaySpecialAlert("REPORT", reportee, cleanMessage, Formatting.RED);

            if (config.enableDiscordPing && config.reportAlertPing) {
                webhook.sendMessage(String.format("`Report: %s reported %s for %s`", reporter, reportee, reason));
            }

            return true;
        }
        return false;
    }

    private boolean isValidTarget(String username) {
        return isValidTarget(username, true);
    }

    private boolean isValidTarget(String username, boolean requireOnline) {
        if (username == null || username.isEmpty())
            return false;

        ConfigManager.Config config = ConfigManager.getConfig();
        for (String ignored : config.ignoredSystemUsernames) {
            if (ignored.equalsIgnoreCase(username)) {
                LOGGER.debug("[ModAssist]: Ignoring system username (Config): {}", username);
                return false;
            }
        }

        if (!requireOnline) {
            return true;
        }

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

    private void displaySpecialAlert(String title, String username, String message, Formatting color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            MutableText prefixText = Text.literal("[" + title + "] ")
                    .setStyle(Style.EMPTY.withColor(color).withBold(true));
            int usernameStart = message.indexOf(username);
            if (usernameStart != -1) {
                String beforeUsername = message.substring(0, usernameStart);
                String afterUsername = message.substring(usernameStart + username.length());
                MutableText beforeText = Text.literal(beforeUsername).setStyle(Style.EMPTY.withColor(Formatting.WHITE));
                MutableText usernameText = Text.literal(username)
                        .setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(true)
                                .withHoverEvent(
                                        new HoverEvent.ShowText(Text.literal("Click for actions on " + username)))
                                .withClickEvent(new ClickEvent.RunCommand("/autochatmod action " + username)));
                MutableText afterText = Text.literal(afterUsername).setStyle(Style.EMPTY.withColor(Formatting.WHITE));
                MutableText fullMessage = prefixText.append(beforeText).append(usernameText).append(afterText);
                client.execute(() -> client.player.sendMessage(fullMessage, false));
            } else {
                MutableText messageText = Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.WHITE)
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click for actions on " + username)))
                        .withClickEvent(new ClickEvent.RunCommand("/autochatmod action " + username)));
                MutableText fullMessage = prefixText.append(messageText);
                client.execute(() -> client.player.sendMessage(fullMessage, false));
            }
        }
    }

    private void playSound(SoundOption sound, float volume, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            try {
                String soundIdString = sound.getSoundId();
                Identifier soundId = Identifier.of(soundIdString);
                SoundEvent soundEvent = SoundEvent.of(soundId);
                PositionedSoundInstance soundInstance = new PositionedSoundInstance(soundEvent, SoundCategory.PLAYERS,
                        volume, pitch, Random.create(), client.player.getBlockPos());
                client.getSoundManager().play(soundInstance);
                LOGGER.debug("[AutoChatMod]: Played sound '{}'", soundIdString);
            } catch (Exception e) {
                LOGGER.error("[AutoChatMod]: Failed to play custom sound.", e);
            }
        }
    }

    // sevengtz/autochatmod/ChatMonitor.java

    private boolean handleRealnameResponse(String message) {
        Matcher matcher = REALNAME_RESPONSE_PATTERN.matcher(message);
        if (matcher.find()) {
            String realUsername = matcher.group(1);
            String originalNick = matcher.group(2);
            String nickKey = originalNick.toLowerCase();

            PendingNickResolution pending = pendingNickResolutions.remove(nickKey);

            LOGGER.info("[AutoChatMod]: Resolved {} -> {}. Opening action menu.", originalNick, realUsername);

            // --- SIMPLIFIED LOGIC: ALWAYS OPEN THE OVERLAY ---
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.execute(() -> {
                    client.player.networkHandler.sendChatCommand("autochatmod action " + realUsername);
                });
            }

            // If this resolution came from an automatic flag, we can still run the other
            // actions
            // like playing an alert sound or showing the [FLAGGED] message.
            boolean actionsExecuted = pending != null && !pending.openActionOnResolve;
            if (actionsExecuted) {
                executeActions(realUsername, pending.originalMessage, pending.isSpam);
            }

            // Also, ensure any pending Discord notifications get sent with the resolved
            // name.
            String pendingDiscordMessage = pendingDiscordNotifications.remove(nickKey);
            if (pendingDiscordMessage != null) {
                // Only send if we didn't just send it in executeActions
                // executeActions sends it if actionsExecuted is true AND !pending.isSpam
                boolean sentInActions = actionsExecuted && !pending.isSpam;

                if (!sentInActions) {
                    // Replace the original nickname with the real username in the Discord message
                    String resolvedDiscordMessage = pendingDiscordMessage.replace(originalNick, realUsername);
                    webhook.sendMessage(resolvedDiscordMessage);
                    LOGGER.info("[AutoChatMod]: Sent resolved Discord notification: {}", resolvedDiscordMessage);
                }
            }

            return true;
        }
        return false;
    }

    private void handleFlaggedMessage(String message, boolean isSpam) {
        UsernameInfo userInfo = extractUsernameInfo(message);
        if (userInfo == null) {
            LOGGER.warn("[AutoChatMod]: Could not extract username from flagged message: {}", message);
            return;
        }

        if (userInfo.isNick) {
            String nick = userInfo.username;
            LOGGER.info("[AutoChatMod]: Flagged message from nick '{}'. Will resolve on user click.", nick);

            List<MessageEntry> similarMessages = null;
            if (isSpam) {
                similarMessages = collectSimilarMessages(message);
            }

            pendingNickResolutions.put(nick.toLowerCase(),
                    new PendingNickResolution(message, nick, isSpam, Instant.now().toEpochMilli(), similarMessages,
                            false));

            ConfigManager.Config config = ConfigManager.getConfig();
            if (config.enableDiscordPing) {
                String alertType = isSpam ? "Spam detected" : "Flagged message";
                String discordMessage = String.format("`%s from %s: %s`", alertType, nick, userInfo.messageContent);

                pendingDiscordNotifications.put(nick.toLowerCase(), discordMessage);

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.execute(() -> {
                        client.player.networkHandler.sendChatCommand("realname " + nick);
                    });
                }
            }
        } else {
            String username = userInfo.username;
            executeActions(username, message, isSpam);
        }
    }

    private List<MessageEntry> collectSimilarMessages(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        List<MessageEntry> similarMessages = new ArrayList<>();
        messageHistory.stream()
                .filter(entry -> calculateSimilarity(message, entry.message) >= config.spamSimilarityThreshold)
                .forEach(similarMessages::add);
        return similarMessages;
    }

    private void executeSpamActions(String username, String originalMessage, List<MessageEntry> similarMessages) {
        ConfigManager.Config config = ConfigManager.getConfig();

        if (config.enableDiscordPing) {
            StringBuilder spamMessages = new StringBuilder("`Spam detected:\n");
            for (MessageEntry entry : similarMessages) {
                String cleanedMessage = extractUsernameAndMessage(entry.message, username);
                spamMessages.append("- ").append(cleanedMessage).append("\n");
            }
            String cleanedOriginal = extractUsernameAndMessage(originalMessage, username);
            spamMessages.append("- ").append(cleanedOriginal).append("`");
            webhook.sendMessage(spamMessages.toString());
        }

        for (MessageEntry entry : similarMessages) {
            displayFlaggedMessage(username, entry.message, true);
        }
    }

    private void executeActions(String username, String originalMessage, boolean isSpam) {
        ConfigManager.Config config = ConfigManager.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        LOGGER.info("[AutoChatMod]: Executing actions for user '{}' on message: {}", username, originalMessage);

        if (config.enableDiscordPing && !isSpam) {
            String extractedContent = extractUsernameAndMessage(originalMessage, username);
            String alertType = isSpam ? "Spam detected" : "Flagged message";
            webhook.sendMessage(
                    String.format("`%s from %s: %s`", alertType, username, extractedContent.split(":", 2)[1].trim()));
        }

        displayFlaggedMessage(username, originalMessage, isSpam);
        playAlertSound();

        if (config.autoOpenOverlayOnFlag) {
            FlagType type = isSpam ? FlagType.SPAM : FlagType.FLAGGED_PHRASE;
            client.execute(() -> AutoChatMod.ACTION_MENU.show(username, type));

            // Check the special condition to see if we should auto-punish
            boolean shouldAutoPunish = config.autoOpenPunishGuiOnFlag && (!isSpam || !config.instantPunishForSpam);
            if (shouldAutoPunish) {
                client.execute(() -> client.player.networkHandler.sendChatCommand("punish " + username));
            }
        }
    }

    private boolean shouldIgnoreMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return true;
        }

        String cleanMessage = stripColorCodes(message);

        // Ensure X-Ray and Report messages are NOT ignored even if they match
        // whitelisted prefixes
        if (XRAY_PATTERN.matcher(cleanMessage).find())
            return false;
        if (REPORT_PATTERN.matcher(cleanMessage).find())
            return false;

        if (cleanMessage.startsWith("[SPAM]") || cleanMessage.startsWith("[FLAGGED]")) {
            LOGGER.debug("[AutoChatMod]: Ignoring already flagged message: {}", message);
            return true;
        }

        if (COREPROTECT_PATTERN.matcher(cleanMessage).find()) {
            LOGGER.debug("[AutoChatMod]: Ignoring CoreProtect message: {}", message);
            return true;
        }

        if (PLAYERS_SLEEPING_PATTERN.matcher(cleanMessage).find()) {
            LOGGER.debug("[AutoChatMod]: Ignoring players sleeping message: {}", message);
            return true;
        }

        ConfigManager.Config config = ConfigManager.getConfig();

        for (String prefix : config.spamWhitelistPrefixes) {
            if (prefix != null && !prefix.trim().isEmpty() && message.startsWith(prefix)) {
                LOGGER.debug("[AutoChatMod]: Ignoring whitelisted prefix [{}] in message: {}", prefix, message);
                return true;
            }
        }

        return false;
    }

    private boolean checkForSpam(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        long currentTime = Instant.now().toEpochMilli();
        if (message.startsWith("[*]")) {
            return false;
        }

        messageHistory.removeIf(entry -> currentTime - entry.timestamp > config.spamTimeWindowSeconds * 1000L);

        List<MessageEntry> similarMessages = new ArrayList<>();
        long similarCount = messageHistory.stream()
                .filter(entry -> {
                    double sim = calculateSimilarity(message, entry.message);
                    boolean isSimilar = sim >= config.spamSimilarityThreshold;
                    if (isSimilar) {
                        similarMessages.add(entry);
                        LOGGER.debug("[AutoChatMod]: Similar message found [{}] ~ sim={}", entry.message, sim);
                    }
                    return isSimilar;
                })
                .count();
        similarCount++;

        long shortIdenticalCount = 0;
        if (message.length() <= 2) {
            shortIdenticalCount = messageHistory.stream()
                    .filter(entry -> entry.message.equals(message))
                    .count();
            shortIdenticalCount++;
        }

        boolean isSpamBySimilarity = similarCount >= config.spamMessageCount;
        boolean isSpamByShortMessages = (message.length() <= 2) && (shortIdenticalCount >= config.spamMessageCount);

        messageHistory.add(new MessageEntry(currentTime, message));

        if (isSpamBySimilarity) {
            LOGGER.info("[AutoChatMod]: Spam detected due to similarity. SimilarCount={}, Threshold={}",
                    similarCount, config.spamMessageCount);

            UsernameInfo userInfo = extractUsernameInfo(message);
            if (userInfo == null || !userInfo.isNick) {
                handleDirectSpam(message, similarMessages);
            }
            return true;
        }

        if (isSpamByShortMessages) {
            LOGGER.info(
                    "[AutoChatMod]: Spam detected due to short, identical messages. ShortIdenticalCount={}, Threshold={}",
                    shortIdenticalCount, config.spamMessageCount);

            UsernameInfo userInfo = extractUsernameInfo(message);
            if (userInfo == null || !userInfo.isNick) {
                handleShortSpam(message);
            }
            return true;
        }

        return false;
    }

    private void handleDirectSpam(String message, List<MessageEntry> similarMessages) {
        ConfigManager.Config config = ConfigManager.getConfig();

        for (MessageEntry entry : similarMessages) {
            flaggedMessages.add(entry.message);
        }
        flaggedMessages.add(message);

        if (config.enableDiscordPing) {
            StringBuilder spamMessages = new StringBuilder("`Spam detected:\n");
            for (MessageEntry entry : similarMessages) {
                String cleanedMessage = extractUsernameAndMessage(entry.message, null);
                spamMessages.append("- ").append(cleanedMessage).append("\n");
            }
            String cleanedCurrent = extractUsernameAndMessage(message, null);
            spamMessages.append("- ").append(cleanedCurrent).append("`");
            webhook.sendMessage(spamMessages.toString());
        }

        messageHistory.clear();
        playAlertSound();
    }

    private void handleShortSpam(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();

        for (MessageEntry entry : messageHistory) {
            if (entry.message.equals(message)) {
                flaggedMessages.add(entry.message);
            }
        }
        flaggedMessages.add(message);

        if (config.enableDiscordPing) {
            String cleanedMessage = extractUsernameAndMessage(message, null);
            webhook.sendMessage("`Short spam detected: " + cleanedMessage + "`");
        }

        messageHistory.clear();
        playAlertSound();
    }

    private boolean checkFlaggedPhrases(String message) {
        if (message.startsWith("[Auth]")) {
            LOGGER.debug("[AutoChatMod]: Skipping phrase check for Auth message: {}", message);
            return false;
        }

        ConfigManager.Config config = ConfigManager.getConfig();
        String lowerMessage = message.toLowerCase();

        for (String phrase : config.whitelistedPhrases) {
            if (phrase != null && !phrase.trim().isEmpty() && lowerMessage.contains(phrase.toLowerCase())) {
                LOGGER.debug("[AutoChatMod]: Phrase [{}] whitelisted in message: {}", phrase, message);
                return false;
            }
        }

        for (String phrase : config.flaggedPhrases) {
            if (phrase != null && !phrase.trim().isEmpty() && lowerMessage.contains(phrase.toLowerCase())) {
                LOGGER.info("[AutoChatMod]: Phrase [{}] matched in message: {}", phrase, message);
                flagMessage(message);
                return true;
            }
        }

        return false;
    }

    private boolean checkFlaggedTerms(String message) {
        String[] words = wordPattern.matcher(message).results()
                .map(match -> match.group())
                .toArray(String[]::new);

        for (String word : words) {
            if (isTermSimilarToFlagged(word)) {
                LOGGER.info("[AutoChatMod]: Word [{}] matched flagged terms in message: {}", word, message);
                flagMessage(message);
                return true;
            }
        }

        return false;
    }

    private boolean isTermSimilarToFlagged(String word) {
        ConfigManager.Config config = ConfigManager.getConfig();
        String lowerWord = word.toLowerCase();

        for (String whitelisted : config.whitelistedTerms) {
            if (whitelisted != null && !whitelisted.trim().isEmpty() && lowerWord.equals(whitelisted.toLowerCase())) {
                LOGGER.debug("[AutoChatMod]: Word [{}] is whitelisted", word);
                return false;
            }
        }

        if (lowerWord.equals("discord.gg/")) {
            LOGGER.info("[AutoChatMod]: Word [{}] flagged as discord link", word);
            return true;
        }

        for (String flaggedTerm : config.flaggedTerms) {
            if (flaggedTerm != null && !flaggedTerm.trim().isEmpty()) {
                double sim = calculateSimilarity(lowerWord, flaggedTerm.toLowerCase());
                if (sim >= config.similarityThreshold) {
                    LOGGER.info("[AutoChatMod]: Word [{}] similar to flagged term [{}] with sim={}", word, flaggedTerm,
                            sim);
                    return true;
                }
            }
        }

        return false;
    }

    private void flagMessage(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        LOGGER.info("[AutoChatMod]: Flagging message: {}", message);

        if (config.enableDiscordPing) {
            String cleanedMessage = extractUsernameAndMessage(message, null);
            webhook.sendMessage("`Flagged message: " + cleanedMessage + "`");
        }

        flaggedMessages.add(message);
        playAlertSound();
    }

    private void displayFlaggedMessage(String finalUsername, String originalMessage, boolean isSpam) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        String cleanMessage = stripColorCodes(originalMessage);
        String prefix = isSpam ? "[SPAM] " : "[FLAGGED] ";

        MutableText prefixText = Text.literal(prefix).setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true));

        int usernameStart = cleanMessage.indexOf(finalUsername);
        if (usernameStart != -1) {
            String beforeUsername = cleanMessage.substring(0, usernameStart);
            String afterUsername = cleanMessage.substring(usernameStart + finalUsername.length());

            MutableText beforeText = Text.literal(beforeUsername);
            MutableText usernameText = Text.literal(finalUsername)
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.YELLOW)
                            .withBold(true)
                            .withHoverEvent(
                                    new HoverEvent.ShowText(Text.literal("Click for actions on " + finalUsername)))
                            .withClickEvent(new ClickEvent.RunCommand("/autochatmod action " + finalUsername)));
            MutableText afterText = Text.literal(afterUsername);

            MutableText fullMessage = prefixText.append(beforeText).append(usernameText).append(afterText);
            client.execute(() -> client.player.sendMessage(fullMessage, false));
        } else {
            MutableText messageText = Text.literal(cleanMessage)
                    .setStyle(Style.EMPTY
                            .withColor(Formatting.YELLOW)
                            .withHoverEvent(
                                    new HoverEvent.ShowText(Text.literal("Click for actions on " + finalUsername)))
                            .withClickEvent(new ClickEvent.RunCommand("/autochatmod action " + finalUsername)));

            MutableText fullMessage = prefixText.append(messageText);
            client.execute(() -> client.player.sendMessage(fullMessage, false));
        }
    }

    private String extractSender(String fullMessage) {
        String cleanMessage = stripColorCodes(fullMessage);

        UsernameInfo userInfo = extractUsernameInfo(cleanMessage);
        if (userInfo == null) {
            Matcher reportMatcher = REPORT_PATTERN.matcher(cleanMessage);
            if (reportMatcher.find()) {
                return reportMatcher.group(1);
            }

            Matcher filteredMatcher = FILTERED_PATTERN.matcher(cleanMessage);
            if (filteredMatcher.find()) {
                String candidate = filteredMatcher.group(1);
                if (candidate.length() >= 2) {
                    return candidate;
                }
            }

            return null;
        }

        if (userInfo.isNick) {
            return null;
        }

        return userInfo.username;
    }

    private void playAlertSound() {

        if (!ConfigManager.getConfig().alertSoundEnabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            try {
                ConfigManager.Config config = ConfigManager.getConfig();
                String soundIdString = config.alertSound.getSoundId();
                Identifier soundId = Identifier.of(soundIdString);
                SoundEvent soundEvent = SoundEvent.of(soundId);

                PositionedSoundInstance soundInstance = new PositionedSoundInstance(
                        soundEvent,
                        SoundCategory.PLAYERS,
                        config.alertSoundVolume, // Use custom volume
                        config.alertSoundPitch, // Use custom pitch
                        Random.create(),
                        client.player.getBlockPos());
                client.getSoundManager().play(soundInstance);
                LOGGER.debug("[AutoChatMod]: Played alert sound '{}'", soundIdString);

            } catch (Exception e) {
                LOGGER.error("[AutoChatMod]: Failed to play custom sound.", e);
            }
        }
    }

    private String stripColorCodes(String input) {
        if (input == null)
            return "";
        return COLOR_CODE_PATTERN.matcher(input).replaceAll("");
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null)
            return 0.0;
        if (s1.equals(s2))
            return 1.0;

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0)
            return 1.0;

        return (maxLen - levenshteinDistance(s1, s2)) / (double) maxLen;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private String extractUsernameAndMessage(String fullMessage, String resolvedUsername) {
        String cleanMessage = stripColorCodes(fullMessage);

        UsernameInfo userInfo = extractUsernameInfo(cleanMessage);
        if (userInfo != null) {
            String displayName = resolvedUsername != null ? resolvedUsername : userInfo.username;
            return displayName + ": " + userInfo.messageContent;
        }

        return cleanMessage;
    }

    private String extractUsernameAndMessage(String fullMessage) {
        return extractUsernameAndMessage(fullMessage, null);
    }

    private static class MessageEntry {
        final long timestamp;
        final String message;

        MessageEntry(long timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
    }
}