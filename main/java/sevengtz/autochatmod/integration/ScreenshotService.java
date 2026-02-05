package sevengtz.autochatmod.integration;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Service for capturing and saving evidence screenshots.
 * Automatically saves screenshots when moderation actions are taken.
 */
public class ScreenshotService {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    private static final String EVIDENCE_FOLDER = "modassist-evidence";
    private static final int MAX_REASON_LENGTH = 50;

    /**
     * Takes a screenshot and saves it as evidence.
     * 
     * @param mutedPlayer The player who was muted
     * @param reason      The reason for the mute
     * @param client      The Minecraft client instance
     */
    public static void takeScreenshot(String mutedPlayer, String reason, MinecraftClient client) {
        LOGGER.info("[ScreenshotService]: takeScreenshot called for player: {}, reason: {}",
                mutedPlayer, reason);

        client.execute(() -> {
            LOGGER.info("[ScreenshotService]: Executing screenshot on client thread");
            try {
                File evidenceDir = createEvidenceDirectory(client);
                String fileName = generateFileName(mutedPlayer, reason);
                File screenshotFile = new File(evidenceDir, fileName);

                LOGGER.info("[ScreenshotService]: Screenshot file path: {}",
                        screenshotFile.getAbsolutePath());

                ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), (nativeImage) -> {
                    LOGGER.info("[ScreenshotService]: Screenshot callback executed");
                    try {
                        nativeImage.writeTo(screenshotFile);
                        nativeImage.close();
                        sendConfirmation(
                                Text.literal("Screenshot saved: " + fileName).formatted(Formatting.GREEN),
                                client);
                    } catch (Exception e) {
                        LOGGER.error("Failed to save evidence screenshot", e);
                        sendConfirmation(
                                Text.literal("Failed to save screenshot!").formatted(Formatting.RED),
                                client);
                    }
                });

            } catch (Exception e) {
                LOGGER.error("Failed to take evidence screenshot", e);
                sendConfirmation(
                        Text.literal("Failed to save screenshot!").formatted(Formatting.RED),
                        client);
            }
        });
    }

    /**
     * Creates the evidence directory if it doesn't exist.
     * 
     * @param client The Minecraft client instance
     * @return The evidence directory File object
     */
    private static File createEvidenceDirectory(MinecraftClient client) {
        File evidenceDir = new File(client.runDirectory, EVIDENCE_FOLDER);
        boolean created = evidenceDir.mkdirs();
        LOGGER.info("[ScreenshotService]: Evidence directory created/exists: {}, Path: {}",
                created, evidenceDir.getAbsolutePath());
        return evidenceDir;
    }

    /**
     * Generates a filename for the screenshot.
     * 
     * @param mutedPlayer The player name
     * @param reason      The reason for the action
     * @return A sanitized filename
     */
    private static String generateFileName(String mutedPlayer, String reason) {
        String sanitizedReason = sanitizeForFilename(reason);
        String timestamp = DATE_FORMAT.format(new Date());
        return String.format("%s_%s_%s.png", mutedPlayer, sanitizedReason, timestamp);
    }

    /**
     * Sanitizes a string for use in a filename.
     * 
     * @param input The input string
     * @return A sanitized string safe for filenames
     */
    private static String sanitizeForFilename(String input) {
        String sanitized = input.replaceAll("[^a-zA-Z0-9.-]", "_");
        if (sanitized.length() > MAX_REASON_LENGTH) {
            sanitized = sanitized.substring(0, MAX_REASON_LENGTH);
        }
        return sanitized;
    }

    /**
     * Sends a confirmation message to the player.
     * 
     * @param message The message to send
     * @param client  The Minecraft client instance
     */
    private static void sendConfirmation(Text message, MinecraftClient client) {
        MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                .append(Text.literal("Evidence").formatted(Formatting.AQUA))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));

        if (client.player != null) {
            client.player.sendMessage(prefix.append(message), false);
        }
    }
}
