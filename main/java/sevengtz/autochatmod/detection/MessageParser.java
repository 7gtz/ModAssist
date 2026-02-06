package sevengtz.autochatmod.detection;

import sevengtz.autochatmod.model.UsernameInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing chat messages and extracting username information.
 * Handles various message formats including standard chat, filtered messages,
 * reports, and nicknames.
 */
public class MessageParser {

    private static final Pattern FILTERED_PRIVATE_MESSAGE_PATTERN = Pattern.compile(
            "^\\[S] \\[[^\\]]+] \\[Filtered] (?:\\[[^\\]]+] )?((?:\\* )?(\\w{2,})) » (?:\\[[^\\]]+] )?(?:\\* )?(\\w{2,}): (.*)$");
    private static final Pattern FILTERED_PATTERN = Pattern.compile(".*\\[Filtered]\\s+(\\w{2,})");
    private static final Pattern REPORT_PATTERN = Pattern.compile(
            "^\\[S\\] \\[[^\\]]+\\] (\\w{2,}) reported (\\w{2,}) for (.*)$");
    private static final Pattern XRAY_PATTERN = Pattern.compile(
            "^X-Ray\\s+\\u25B6\\s+(\\w{2,})\\s+found\\s+x(\\d+)\\s+(.+)$");
    private static final Pattern SERVER_FILTERED_PATTERN = Pattern.compile(
            "^\\[S] \\[\\w+] \\[Filtered] (\\w{2,})");

    /**
     * Extracts username information from a chat message.
     * 
     * @param message The message to parse (should already have color codes
     *                stripped)
     * @return UsernameInfo if a username was found, null otherwise
     */
    public UsernameInfo extractUsernameInfo(String message) {
        // Check for filtered private messages first
        Matcher privateMatcher = FILTERED_PRIVATE_MESSAGE_PATTERN.matcher(message);
        if (privateMatcher.find()) {
            String senderWithPossibleNick = privateMatcher.group(1);
            String actualSender = privateMatcher.group(2);
            String messageContent = privateMatcher.group(4);
            boolean senderIsNick = senderWithPossibleNick.startsWith("* ");
            return new UsernameInfo(actualSender, senderIsNick, messageContent);
        }

        // Check for server filtered messages
        Matcher serverFilteredMatcher = SERVER_FILTERED_PATTERN.matcher(message);
        if (serverFilteredMatcher.find()) {
            String username = serverFilteredMatcher.group(1);
            String messageContent = message.substring(serverFilteredMatcher.end()).trim();
            return UsernameInfo.regular(username, messageContent);
        }

        // Standard chat message parsing (look for colon separator)
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

        // Check for report pattern
        Matcher reportMatcher = REPORT_PATTERN.matcher(message);
        if (reportMatcher.find()) {
            String username = reportMatcher.group(1);
            return UsernameInfo.regular(username, message);
        }

        // Check for filtered pattern
        Matcher filteredMatcher = FILTERED_PATTERN.matcher(message);
        if (filteredMatcher.find()) {
            String username = filteredMatcher.group(1);
            if (username.length() >= 2) {
                return UsernameInfo.regular(username, message);
            }
        }

        return null;
    }

    /**
     * Parses a username from the text before a colon in a chat message.
     * 
     * @param beforeColon    The text before the colon
     * @param messageContent The message content after the colon
     * @return UsernameInfo if valid username found, null otherwise
     */
    private UsernameInfo parseUsernameFromBeforeColon(String beforeColon, String messageContent) {
        String[] words = beforeColon.split("\\s+");
        if (words.length == 0) {
            return null;
        }

        String lastWord = words[words.length - 1].trim();

        // Determine if it's a nickname by checking for a preceding '*'
        boolean isNick = words.length >= 2 && words[words.length - 2].equals("*");

        // Validate username
        boolean isValidUsername;
        if (isNick) {
            isValidUsername = lastWord.length() >= 1 && lastWord.matches("\\w+");
        } else {
            isValidUsername = lastWord.length() >= 2 && lastWord.matches("\\w{2,}");
        }

        if (!isValidUsername) {
            return null;
        }

        // Prevent parsing usernames inside rank brackets like [Admin]
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

        return new UsernameInfo(lastWord, isNick, messageContent);
    }

    /**
     * Attempts to extract information from an X-Ray log message.
     * 
     * @param message The message to check
     * @return A string array [username, count, oreType] if matched, null otherwise
     */
    public String[] parseXRayMessage(String message) {
        Matcher matcher = XRAY_PATTERN.matcher(message);
        if (matcher.find()) {
            return new String[] {
                    matcher.group(1), // username
                    matcher.group(2), // count
                    matcher.group(3) // ore type
            };
        }
        return null;
    }

    /**
     * Attempts to extract information from a report message.
     * 
     * @param message The message to check
     * @return A string array [reporter, reportee, reason] if matched, null
     *         otherwise
     */
    public String[] parseReportMessage(String message) {
        Matcher matcher = REPORT_PATTERN.matcher(message);
        if (matcher.find()) {
            return new String[] {
                    matcher.group(1), // reporter
                    matcher.group(2), // reportee
                    matcher.group(3) // reason
            };
        }
        return null;
    }

    /**
     * Gets the X-Ray pattern for external use (e.g., making messages clickable).
     * 
     * @return The compiled X-Ray pattern
     */
    public Pattern getXRayPattern() {
        return XRAY_PATTERN;
    }

    /**
     * Gets the Report pattern for external use.
     * 
     * @return The compiled Report pattern
     */
    public Pattern getReportPattern() {
        return REPORT_PATTERN;
    }
}
