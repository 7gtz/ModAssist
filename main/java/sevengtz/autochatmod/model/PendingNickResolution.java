package sevengtz.autochatmod.model;

import java.util.List;

/**
 * Represents a pending nickname resolution request.
 * When a flagged message comes from a nickname, we need to resolve the real
 * username
 * before taking moderation actions.
 */
public record PendingNickResolution(
        String originalMessage,
        String nick,
        boolean isSpam,
        long timestamp,
        List<MessageEntry> similarMessages,
        boolean openActionOnResolve) {

    /**
     * Creates a pending resolution for an action-only request (user clicked on
     * nickname).
     * 
     * @param originalMessage The original message
     * @param nick            The nickname to resolve
     * @return A new PendingNickResolution configured for action on resolve
     */
    public static PendingNickResolution forAction(String originalMessage, String nick) {
        return new PendingNickResolution(
                originalMessage,
                nick,
                false,
                System.currentTimeMillis(),
                null,
                true);
    }

    /**
     * Creates a pending resolution for a flagged message.
     * 
     * @param originalMessage The original message
     * @param nick            The nickname to resolve
     * @param isSpam          Whether the message was flagged as spam
     * @param similarMessages List of similar messages if spam
     * @return A new PendingNickResolution configured for flagged message handling
     */
    public static PendingNickResolution forFlagged(
            String originalMessage,
            String nick,
            boolean isSpam,
            List<MessageEntry> similarMessages) {
        return new PendingNickResolution(
                originalMessage,
                nick,
                isSpam,
                System.currentTimeMillis(),
                similarMessages,
                false);
    }
}
