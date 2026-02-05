package sevengtz.autochatmod.model;

/**
 * Enumeration of different types of flagged content.
 * Used to categorize why a message or user was flagged for moderation.
 */
public enum FlagType {
    /** Message was detected as spam */
    SPAM,

    /** Message contained a flagged term (single word) */
    FLAGGED_TERM,

    /** Message contained a flagged phrase */
    FLAGGED_PHRASE,

    /** User manually clicked on a username in chat */
    MANUAL_CLICK,

    /** User selected a player by looking at them and pressing keybind */
    MANUAL_SELECT,

    /** X-Ray activity was detected */
    XRAY,

    /** A player report was detected */
    REPORT
}
