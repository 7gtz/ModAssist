package sevengtz.autochatmod.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for detecting X-Ray activity based on server logs.
 * Tracks ore discovery patterns and triggers alerts based on configurable
 * thresholds.
 */
public class XRayDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    private final Map<String, List<Long>> xRayTracker = new ConcurrentHashMap<>();
    private final MessageParser messageParser;

    public XRayDetector(MessageParser messageParser) {
        this.messageParser = messageParser;
    }

    /**
     * Result of an X-Ray detection check.
     */
    public record XRayResult(
            boolean isAlert,
            String username,
            String count,
            String oreType,
            int totalInWindow) {
        public static XRayResult noMatch() {
            return new XRayResult(false, null, null, null, 0);
        }

        public static XRayResult tracked(String username, String count, String oreType, int total) {
            return new XRayResult(false, username, count, oreType, total);
        }

        public static XRayResult alert(String username, String count, String oreType, int total) {
            return new XRayResult(true, username, count, oreType, total);
        }
    }

    /**
     * Checks a message for X-Ray log activity.
     * 
     * @param message The message to check (color codes already stripped)
     * @param config  The current mod configuration
     * @return XRayResult with detection and alert information
     */
    public XRayResult checkForXRay(String message, ModConfig config) {
        String[] xrayInfo = messageParser.parseXRayMessage(message);
        if (xrayInfo == null) {
            return XRayResult.noMatch();
        }

        String username = xrayInfo[0];
        String count = xrayInfo[1];
        String oreType = xrayInfo[2];

        long now = System.currentTimeMillis();
        long windowMillis = config.xrayTimeWindowSeconds * 1000L;

        // Get or create history for this user
        List<Long> history = xRayTracker.computeIfAbsent(username, k -> new ArrayList<>());
        history.add(now);

        // Remove entries outside the time window
        history.removeIf(timestamp -> now - timestamp > windowMillis);

        int currentCount = history.size();

        if (currentCount >= config.xrayAlertThreshold) {
            LOGGER.info("[AutoChatMod]: X-Ray Alert triggered for {}", username);
            history.clear(); // Reset after alerting
            return XRayResult.alert(username, count, oreType, currentCount);
        }

        return XRayResult.tracked(username, count, oreType, currentCount);
    }

    /**
     * Clears the tracking history for a specific user.
     * 
     * @param username The username to clear history for
     */
    public void clearUserHistory(String username) {
        xRayTracker.remove(username);
    }

    /**
     * Clears all tracking history.
     */
    public void clearAllHistory() {
        xRayTracker.clear();
    }
}
