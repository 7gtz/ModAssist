package sevengtz.autochatmod.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.ModConfig;

import java.util.regex.Pattern;

/**
 * Service for determining if messages should be ignored from processing.
 * Handles various filters like system messages, prefixes, and already-flagged
 * content.
 */
public class MessageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");

    private static final Pattern COREPROTECT_PATTERN = Pattern.compile("^\\d+\\.\\d{2}/[dmh]\\s+ago.*");
    private static final Pattern PLAYERS_SLEEPING_PATTERN = Pattern.compile("^\\d+/\\d+\\s+players\\s+sleeping.*");
    private static final Pattern REALNAME_RESPONSE_PATTERN = Pattern.compile(
            "^\\[\\*\\]\\s+(\\w{2,})\\s+is nicknamed as\\s+(\\w{2,})$");

    private final MessageParser messageParser;

    public MessageFilter(MessageParser messageParser) {
        this.messageParser = messageParser;
    }

    /**
     * Determines if a message should be ignored from moderation processing.
     * 
     * @param message The message to check
     * @param config  The current mod configuration
     * @return true if the message should be ignored
     */
    public boolean shouldIgnoreMessage(String message, ModConfig config) {
        if (message == null || message.trim().isEmpty()) {
            return true;
        }

        String cleanMessage = sevengtz.autochatmod.util.StringUtils.stripColorCodes(message);

        // X-Ray and Report messages should NOT be ignored
        if (messageParser.parseXRayMessage(cleanMessage) != null) {
            return false;
        }
        if (messageParser.parseReportMessage(cleanMessage) != null) {
            return false;
        }

        // Ignore already flagged messages
        if (cleanMessage.startsWith("[SPAM]") || cleanMessage.startsWith("[FLAGGED]")) {
            LOGGER.debug("[AutoChatMod]: Ignoring already flagged message: {}", message);
            return true;
        }

        // Ignore CoreProtect messages
        if (COREPROTECT_PATTERN.matcher(cleanMessage).find()) {
            LOGGER.debug("[AutoChatMod]: Ignoring CoreProtect message: {}", message);
            return true;
        }

        // Ignore sleeping messages
        if (PLAYERS_SLEEPING_PATTERN.matcher(cleanMessage).find()) {
            LOGGER.debug("[AutoChatMod]: Ignoring players sleeping message: {}", message);
            return true;
        }

        // Check whitelisted prefixes
        for (String prefix : config.spamWhitelistPrefixes) {
            if (prefix != null && !prefix.trim().isEmpty() && message.startsWith(prefix)) {
                LOGGER.debug("[AutoChatMod]: Ignoring whitelisted prefix [{}] in message: {}",
                        prefix, message);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a message is a realname response.
     * 
     * @param message The message to check
     * @return The matched pattern result or null if not a realname response
     */
    public String[] parseRealnameResponse(String message) {
        var matcher = REALNAME_RESPONSE_PATTERN.matcher(message);
        if (matcher.find()) {
            return new String[] {
                    matcher.group(1), // real username
                    matcher.group(2) // nickname
            };
        }
        return null;
    }
}
