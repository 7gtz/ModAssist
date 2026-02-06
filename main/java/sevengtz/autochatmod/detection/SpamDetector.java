package sevengtz.autochatmod.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.ModConfig;
import sevengtz.autochatmod.model.MessageEntry;
import sevengtz.autochatmod.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service for detecting spam in chat messages.
 * Uses message similarity and frequency analysis to identify spam patterns.
 */
public class SpamDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    // Track message history per username
    private final Map<String, Queue<MessageEntry>> userMessageHistory = new ConcurrentHashMap<>();

    /**
     * Result of a spam check operation.
     */
    public record SpamCheckResult(
            boolean isSpam,
            boolean isSimilaritySpam,
            boolean isShortMessageSpam,
            List<MessageEntry> similarMessages) {
        public static SpamCheckResult notSpam() {
            return new SpamCheckResult(false, false, false, List.of());
        }

        public static SpamCheckResult similaritySpam(List<MessageEntry> similarMessages) {
            return new SpamCheckResult(true, true, false, similarMessages);
        }

        public static SpamCheckResult shortMessageSpam() {
            return new SpamCheckResult(true, false, true, List.of());
        }
    }

    /**
     * Checks if a message is spam based on configured thresholds.
     * 
     * @param message  The message to check (color codes already stripped)
     * @param username The username of the sender
     * @param config   The current mod configuration
     * @return SpamCheckResult with detection results
     */
    public SpamCheckResult checkForSpam(String message, String username, ModConfig config) {
        long currentTime = System.currentTimeMillis();

        // Skip realname responses
        if (message.startsWith("[*]")) {
            return SpamCheckResult.notSpam();
        }

        // Get or create history for this user
        Queue<MessageEntry> history = userMessageHistory.computeIfAbsent(username, k -> new ConcurrentLinkedQueue<>());

        // Clean up old messages outside the time window
        long windowMillis = config.spamTimeWindowSeconds * 1000L;
        history.removeIf(entry -> !entry.isWithinWindow(windowMillis, currentTime));

        // Check for similar messages
        List<MessageEntry> similarMessages = new ArrayList<>();
        long similarCount = history.stream()
                .filter(entry -> {
                    double similarity = StringUtils.calculateSimilarity(message, entry.message());
                    boolean isSimilar = similarity >= config.spamSimilarityThreshold;
                    if (isSimilar) {
                        similarMessages.add(entry);
                        LOGGER.debug("[AutoChatMod]: Similar message found for {} [{}] ~ sim={}",
                                username, entry.message(), similarity);
                    }
                    return isSimilar;
                })
                .count();
        similarCount++; // Include current message

        // Check for short identical messages
        long shortIdenticalCount = 0;
        if (message.length() <= 2) {
            shortIdenticalCount = history.stream()
                    .filter(entry -> entry.message().equals(message))
                    .count();
            shortIdenticalCount++;
        }

        // Determine spam type
        boolean isSpamBySimilarity = similarCount >= config.spamMessageCount;
        boolean isSpamByShortMessages = (message.length() <= 2) &&
                (shortIdenticalCount >= config.spamMessageCount);

        // Add current message to history
        history.add(MessageEntry.now(message));

        if (isSpamBySimilarity) {
            LOGGER.info("[AutoChatMod]: Spam detected for {} due to similarity. SimilarCount={}, Threshold={}",
                    username, similarCount, config.spamMessageCount);
            return SpamCheckResult.similaritySpam(similarMessages);
        }

        if (isSpamByShortMessages) {
            LOGGER.info("[AutoChatMod]: Spam detected for {} due to short, identical messages. Count={}, Threshold={}",
                    username, shortIdenticalCount, config.spamMessageCount);
            return SpamCheckResult.shortMessageSpam();
        }

        return SpamCheckResult.notSpam();
    }

    /**
     * Collects messages similar to the given message from history for a specific
     * user.
     * 
     * @param message   The message to find similarities for
     * @param username  The username to check history for
     * @param threshold The similarity threshold
     * @return List of similar message entries
     */
    public List<MessageEntry> collectSimilarMessages(String message, String username, double threshold) {
        List<MessageEntry> similarMessages = new ArrayList<>();
        Queue<MessageEntry> history = userMessageHistory.get(username);

        if (history != null) {
            history.stream()
                    .filter(entry -> StringUtils.calculateSimilarity(message, entry.message()) >= threshold)
                    .forEach(similarMessages::add);
        }
        return similarMessages;
    }

    /**
     * Clears the message history for a specific user.
     * Called after spam is detected to prevent re-detection.
     * 
     * @param username The username to clear history for
     */
    public void clearHistory(String username) {
        userMessageHistory.remove(username);
    }

    /**
     * Gets the current message history for a user (for testing/debugging).
     * 
     * @return An unmodifiable view of the message history or empty list
     */
    public List<MessageEntry> getMessageHistory(String username) {
        Queue<MessageEntry> history = userMessageHistory.get(username);
        return history != null ? List.copyOf(history) : List.of();
    }
}
