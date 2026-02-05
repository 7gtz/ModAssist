package sevengtz.autochatmod.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.ModConfig;
import sevengtz.autochatmod.model.MessageEntry;
import sevengtz.autochatmod.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service for detecting spam in chat messages.
 * Uses message similarity and frequency analysis to identify spam patterns.
 */
public class SpamDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    private final Queue<MessageEntry> messageHistory = new ConcurrentLinkedQueue<>();

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
     * @param message The message to check (color codes already stripped)
     * @param config  The current mod configuration
     * @return SpamCheckResult with detection results
     */
    public SpamCheckResult checkForSpam(String message, ModConfig config) {
        long currentTime = System.currentTimeMillis();

        // Skip realname responses
        if (message.startsWith("[*]")) {
            return SpamCheckResult.notSpam();
        }

        // Clean up old messages outside the time window
        long windowMillis = config.spamTimeWindowSeconds * 1000L;
        messageHistory.removeIf(entry -> !entry.isWithinWindow(windowMillis, currentTime));

        // Check for similar messages
        List<MessageEntry> similarMessages = new ArrayList<>();
        long similarCount = messageHistory.stream()
                .filter(entry -> {
                    double similarity = StringUtils.calculateSimilarity(message, entry.message());
                    boolean isSimilar = similarity >= config.spamSimilarityThreshold;
                    if (isSimilar) {
                        similarMessages.add(entry);
                        LOGGER.debug("[AutoChatMod]: Similar message found [{}] ~ sim={}",
                                entry.message(), similarity);
                    }
                    return isSimilar;
                })
                .count();
        similarCount++; // Include current message

        // Check for short identical messages
        long shortIdenticalCount = 0;
        if (message.length() <= 2) {
            shortIdenticalCount = messageHistory.stream()
                    .filter(entry -> entry.message().equals(message))
                    .count();
            shortIdenticalCount++;
        }

        // Determine spam type
        boolean isSpamBySimilarity = similarCount >= config.spamMessageCount;
        boolean isSpamByShortMessages = (message.length() <= 2) &&
                (shortIdenticalCount >= config.spamMessageCount);

        // Add current message to history
        messageHistory.add(MessageEntry.now(message));

        if (isSpamBySimilarity) {
            LOGGER.info("[AutoChatMod]: Spam detected due to similarity. SimilarCount={}, Threshold={}",
                    similarCount, config.spamMessageCount);
            return SpamCheckResult.similaritySpam(similarMessages);
        }

        if (isSpamByShortMessages) {
            LOGGER.info("[AutoChatMod]: Spam detected due to short, identical messages. Count={}, Threshold={}",
                    shortIdenticalCount, config.spamMessageCount);
            return SpamCheckResult.shortMessageSpam();
        }

        return SpamCheckResult.notSpam();
    }

    /**
     * Collects messages similar to the given message from history.
     * 
     * @param message   The message to find similarities for
     * @param threshold The similarity threshold
     * @return List of similar message entries
     */
    public List<MessageEntry> collectSimilarMessages(String message, double threshold) {
        List<MessageEntry> similarMessages = new ArrayList<>();
        messageHistory.stream()
                .filter(entry -> StringUtils.calculateSimilarity(message, entry.message()) >= threshold)
                .forEach(similarMessages::add);
        return similarMessages;
    }

    /**
     * Clears the message history.
     * Called after spam is detected to prevent re-detection.
     */
    public void clearHistory() {
        messageHistory.clear();
    }

    /**
     * Gets the current message history (for testing/debugging).
     * 
     * @return An unmodifiable view of the message history
     */
    public List<MessageEntry> getMessageHistory() {
        return List.copyOf(messageHistory);
    }
}
