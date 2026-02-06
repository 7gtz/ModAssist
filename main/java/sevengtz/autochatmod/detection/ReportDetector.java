package sevengtz.autochatmod.detection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for detecting player reports.
 * Parses report messages and provides structured information about reports.
 */
public class ReportDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    private final MessageParser messageParser;

    public ReportDetector(MessageParser messageParser) {
        this.messageParser = messageParser;
    }

    /**
     * Result of a report detection check.
     */
    public record ReportResult(
            boolean isReport,
            String reporter,
            String reportee,
            String reason) {
        public static ReportResult noMatch() {
            return new ReportResult(false, null, null, null);
        }

        public static ReportResult found(String reporter, String reportee, String reason) {
            return new ReportResult(true, reporter, reportee, reason);
        }
    }

    /**
     * Checks a message for a player report.
     * 
     * @param message The message to check (color codes already stripped)
     * @return ReportResult with detection information
     */
    public ReportResult checkForReport(String message) {
        String[] reportInfo = messageParser.parseReportMessage(message);
        if (reportInfo == null) {
            return ReportResult.noMatch();
        }

        String reporter = reportInfo[0];
        String reportee = reportInfo[1];
        String reason = reportInfo[2];

        LOGGER.info("[AutoChatMod]: Report detected: {} reported {} for {}",
                reporter, reportee, reason);

        return ReportResult.found(reporter, reportee, reason);
    }
}
