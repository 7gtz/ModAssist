package sevengtz.autochatmod.model;

/**
 * Represents a chat message entry with timestamp for tracking message history.
 * Used primarily for spam detection to track messages within a time window.
 */
public record MessageEntry(long timestamp, String message) {

    /**
     * Creates a new MessageEntry with the current timestamp.
     * 
     * @param message The message content
     * @return A new MessageEntry with current system time
     */
    public static MessageEntry now(String message) {
        return new MessageEntry(System.currentTimeMillis(), message);
    }

    /**
     * Checks if this message entry is within the given time window.
     * 
     * @param windowMillis The time window in milliseconds
     * @param currentTime  The current time to compare against
     * @return true if the message is within the time window
     */
    public boolean isWithinWindow(long windowMillis, long currentTime) {
        return currentTime - timestamp <= windowMillis;
    }
}
