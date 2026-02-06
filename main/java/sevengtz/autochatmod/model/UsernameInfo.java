package sevengtz.autochatmod.model;

/**
 * Container for username information extracted from chat messages.
 * Holds the username, whether it's a nickname, and the message content.
 */
public record UsernameInfo(String username, boolean isNick, String messageContent) {

    /**
     * Creates a UsernameInfo for a regular (non-nickname) username.
     * 
     * @param username       The username
     * @param messageContent The message content
     * @return A new UsernameInfo with isNick=false
     */
    public static UsernameInfo regular(String username, String messageContent) {
        return new UsernameInfo(username, false, messageContent);
    }

    /**
     * Creates a UsernameInfo for a nickname.
     * 
     * @param nickname       The nickname
     * @param messageContent The message content
     * @return A new UsernameInfo with isNick=true
     */
    public static UsernameInfo nickname(String nickname, String messageContent) {
        return new UsernameInfo(nickname, true, messageContent);
    }
}
