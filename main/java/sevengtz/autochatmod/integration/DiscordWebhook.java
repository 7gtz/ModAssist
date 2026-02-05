package sevengtz.autochatmod.integration;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.ConfigManager;
import sevengtz.autochatmod.config.ModConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending notifications to Discord via webhooks.
 * Handles async HTTP requests to prevent blocking the game thread.
 */
public class DiscordWebhook {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoChatMod");
    private final HttpClient httpClient;

    public DiscordWebhook() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends a message to the configured Discord webhook.
     * Automatically appends user mention if configured.
     * 
     * @param message The message to send
     */
    public void sendMessage(String message) {
        ModConfig config = ConfigManager.getConfig();

        if (!isWebhookConfigured(config)) {
            LOGGER.warn("Discord webhook URL not configured");
            return;
        }

        String finalMessage = appendMention(message, config);
        sendMessageAsync(finalMessage, config.webhookUrl);
    }

    /**
     * Sends a message without the user mention.
     * 
     * @param message The message to send
     */
    public void sendMessageWithoutMention(String message) {
        ModConfig config = ConfigManager.getConfig();

        if (!isWebhookConfigured(config)) {
            LOGGER.warn("Discord webhook URL not configured");
            return;
        }

        sendMessageAsync(message, config.webhookUrl);
    }

    /**
     * Checks if the webhook is properly configured.
     * 
     * @param config The configuration to check
     * @return true if webhook URL is configured
     */
    private boolean isWebhookConfigured(ModConfig config) {
        return config.webhookUrl != null &&
                !config.webhookUrl.isEmpty() &&
                !config.webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE");
    }

    /**
     * Appends user mention to message if configured.
     * 
     * @param message The original message
     * @param config  The configuration
     * @return Message with mention appended if configured
     */
    private String appendMention(String message, ModConfig config) {
        if (config.enableDiscordPing &&
                config.userMentionId != null &&
                !config.userMentionId.equals("YOUR ID HERE")) {
            return message + "\n<@" + config.userMentionId + ">";
        }
        return message;
    }

    /**
     * Sends a message asynchronously to avoid blocking.
     * 
     * @param message    The message to send
     * @param webhookUrl The webhook URL to send to
     */
    private void sendMessageAsync(String message, String webhookUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("content", message);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200 && response.statusCode() != 204) {
                    LOGGER.warn("Failed to send Discord message. Status: {}",
                            response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error sending Discord message", e);
            }
        });
    }

    /**
     * Formats an alert message for Discord.
     * 
     * @param alertType The type of alert (e.g., "Spam detected", "Flagged message")
     * @param username  The username involved
     * @param content   The message content
     * @return Formatted Discord message
     */
    public static String formatAlert(String alertType, String username, String content) {
        return String.format("`%s from %s: %s`", alertType, username, content);
    }

    /**
     * Formats an X-Ray alert message for Discord.
     * 
     * @param username The username
     * @param count    The ore count
     * @param oreType  The type of ore found
     * @return Formatted Discord message
     */
    public static String formatXRayAlert(String username, String count, String oreType) {
        return String.format("`X-Ray Alert: %s found x%s %s`", username, count, oreType);
    }

    /**
     * Formats a report alert message for Discord.
     * 
     * @param reporter The reporter username
     * @param reportee The reported username
     * @param reason   The report reason
     * @return Formatted Discord message
     */
    public static String formatReportAlert(String reporter, String reportee, String reason) {
        return String.format("`Report: %s reported %s for %s`", reporter, reportee, reason);
    }
}
