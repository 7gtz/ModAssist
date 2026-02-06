package sevengtz.autochatmod.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.ModConfig;
import sevengtz.autochatmod.util.StringUtils;

/**
 * Service for detecting flagged terms and phrases in chat messages.
 * Uses configurable lists of flagged words and phrases with similarity
 * matching.
 */
public class TermDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");

    /**
     * Result of a term/phrase detection.
     */
    public record DetectionResult(
            boolean isDetected,
            DetectionType type,
            String matchedTerm) {
        public enum DetectionType {
            NONE,
            TERM,
            PHRASE
        }

        public static DetectionResult none() {
            return new DetectionResult(false, DetectionType.NONE, null);
        }

        public static DetectionResult term(String matchedTerm) {
            return new DetectionResult(true, DetectionType.TERM, matchedTerm);
        }

        public static DetectionResult phrase(String matchedPhrase) {
            return new DetectionResult(true, DetectionType.PHRASE, matchedPhrase);
        }
    }

    /**
     * Checks a message for flagged phrases.
     * 
     * @param message The message to check
     * @param config  The current mod configuration
     * @return DetectionResult indicating if a phrase was found
     */
    public DetectionResult checkFlaggedPhrases(String message, ModConfig config) {
        // Skip [Auth] messages
        if (message.startsWith("[Auth]")) {
            LOGGER.debug("[AutoChatMod]: Skipping phrase check for Auth message: {}", message);
            return DetectionResult.none();
        }

        String lowerMessage = message.toLowerCase();

        // Check whitelisted phrases first
        for (String phrase : config.whitelistedPhrases) {
            if (phrase != null && !phrase.trim().isEmpty() &&
                    lowerMessage.contains(phrase.toLowerCase())) {
                LOGGER.debug("[AutoChatMod]: Phrase [{}] whitelisted in message: {}", phrase, message);
                return DetectionResult.none();
            }
        }

        // Check flagged phrases
        for (String phrase : config.flaggedPhrases) {
            if (phrase != null && !phrase.trim().isEmpty() &&
                    lowerMessage.contains(phrase.toLowerCase())) {
                LOGGER.info("[AutoChatMod]: Phrase [{}] matched in message: {}", phrase, message);
                return DetectionResult.phrase(phrase);
            }
        }

        return DetectionResult.none();
    }

    /**
     * Checks a message for flagged terms (individual words).
     * 
     * @param message The message to check
     * @param config  The current mod configuration
     * @return DetectionResult indicating if a term was found
     */
    public DetectionResult checkFlaggedTerms(String message, ModConfig config) {
        String[] words = StringUtils.extractWords(message);

        for (String word : words) {
            String matchedTerm = findSimilarFlaggedTerm(word, config);
            if (matchedTerm != null) {
                LOGGER.info("[AutoChatMod]: Word [{}] matched flagged terms in message: {}",
                        word, message);
                return DetectionResult.term(matchedTerm);
            }
        }

        return DetectionResult.none();
    }

    /**
     * Checks if a word is similar to any flagged term.
     * 
     * @param word   The word to check
     * @param config The current mod configuration
     * @return The matched flagged term, or null if none found
     */
    private String findSimilarFlaggedTerm(String word, ModConfig config) {
        String lowerWord = word.toLowerCase();

        // Check whitelisted terms first
        for (String whitelisted : config.whitelistedTerms) {
            if (whitelisted != null && !whitelisted.trim().isEmpty() &&
                    lowerWord.equals(whitelisted.toLowerCase())) {
                LOGGER.debug("[AutoChatMod]: Word [{}] is whitelisted", word);
                return null;
            }
        }

        // Special case for Discord links
        if (lowerWord.equals("discord.gg/")) {
            LOGGER.info("[AutoChatMod]: Word [{}] flagged as discord link", word);
            return "discord.gg/";
        }

        // Check against flagged terms with similarity
        for (String flaggedTerm : config.flaggedTerms) {
            if (flaggedTerm != null && !flaggedTerm.trim().isEmpty()) {
                double similarity = StringUtils.calculateSimilarity(lowerWord, flaggedTerm.toLowerCase());
                if (similarity >= config.similarityThreshold) {
                    LOGGER.info("[AutoChatMod]: Word [{}] similar to flagged term [{}] with sim={}",
                            word, flaggedTerm, similarity);
                    return flaggedTerm;
                }
            }
        }

        return null;
    }
}
