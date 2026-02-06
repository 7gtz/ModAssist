package sevengtz.autochatmod.util;

import java.util.regex.Pattern;

/**
 * Utility class for string manipulation operations.
 * Provides methods for text processing, similarity calculations, and pattern
 * matching.
 */
public final class StringUtils {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)§.");
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");

    private StringUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Strips Minecraft color codes from a string.
     * 
     * @param input The input string
     * @return The string with color codes removed
     */
    public static String stripColorCodes(String input) {
        if (input == null)
            return "";
        return COLOR_CODE_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Calculates the similarity between two strings using Levenshtein distance.
     * 
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity score between 0.0 and 1.0
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null)
            return 0.0;
        if (s1.equals(s2))
            return 1.0;

        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0)
            return 1.0;

        return (maxLen - levenshteinDistance(s1, s2)) / (double) maxLen;
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     * 
     * @param s1 First string
     * @param s2 Second string
     * @return The edit distance between the strings
     */
    public static int levenshteinDistance(String s1, String s2) {
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
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Extracts all words from a string.
     * 
     * @param input The input string
     * @return Array of words found in the string
     */
    public static String[] extractWords(String input) {
        if (input == null)
            return new String[0];
        return WORD_PATTERN.matcher(input).results()
                .map(match -> match.group())
                .toArray(String[]::new);
    }

    /**
     * Checks if a string is null or empty (after trimming).
     * 
     * @param str The string to check
     * @return true if the string is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
