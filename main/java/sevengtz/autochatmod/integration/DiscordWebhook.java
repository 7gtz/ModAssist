package sevengtz.autochatmod.integration;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.ConfigManager;
import sevengtz.autochatmod.config.ModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
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

    public void sendMessage(String message) {
        ModConfig config = ConfigManager.getConfig();
        if (!isWebhookConfigured(config.webhookUrl))
            return;

        String finalMessage = appendMention(message, config);
        JsonObject payload = new JsonObject();
        payload.addProperty("content", finalMessage);

        sendPayloadAsync(config.webhookUrl, payload);
    }

    public void sendXRayAlert(String username, String count, String oreType) {
        ModConfig config = ConfigManager.getConfig();
        if (config.useCustomBot) {
            if (!isWebhookConfigured(config.customBotUrl))
                return;
            String url = constructBotUrl(config.customBotUrl, "/api/xray");

            JsonObject payload = new JsonObject();
            payload.addProperty("type", "xray");
            payload.addProperty("username", username);
            payload.addProperty("count", count);
            payload.addProperty("ore", oreType);
            payload.addProperty("mentionId", config.userMentionId);
            sendPayloadAsync(url, payload);
        } else {
            sendMessage(formatXRayAlert(username, count, oreType));
        }
    }

    public void sendReportAlert(String reporter, String reportee, String reason) {
        ModConfig config = ConfigManager.getConfig();
        if (config.useCustomBot) {
            if (!isWebhookConfigured(config.customBotUrl))
                return;
            String url = constructBotUrl(config.customBotUrl, "/api/report");

            JsonObject payload = new JsonObject();
            payload.addProperty("type", "report");
            payload.addProperty("reporter", reporter);
            payload.addProperty("reportee", reportee);
            payload.addProperty("reason", reason);
            payload.addProperty("mentionId", config.userMentionId);
            sendPayloadAsync(url, payload);
        } else {
            sendMessage(formatReportAlert(reporter, reportee, reason));
        }
    }

    public void sendSpamAlert(String username, String content, List<String> history) {
        ModConfig config = ConfigManager.getConfig();
        if (config.useCustomBot) {
            if (!isWebhookConfigured(config.customBotUrl))
                return;
            String url = constructBotUrl(config.customBotUrl, "/api/spam");

            JsonObject payload = new JsonObject();
            payload.addProperty("type", "spam");
            payload.addProperty("username", username);
            payload.addProperty("content", content);
            // Simplified history transmission
            StringBuilder hist = new StringBuilder();
            for (String s : history)
                hist.append(s).append("\n");
            payload.addProperty("history", hist.toString());
            payload.addProperty("mentionId", config.userMentionId);

            sendPayloadAsync(url, payload);
        } else {
            // Fallback to text
            StringBuilder msg = new StringBuilder("`Spam detected from " + username + ":\n");
            for (String s : history)
                msg.append("- ").append(s).append("\n");
            msg.append("- ").append(content).append("`");
            sendMessage(msg.toString());
        }
    }

    public void sendFlaggedAlert(String username, String content) {
        ModConfig config = ConfigManager.getConfig();
        if (config.useCustomBot) {
            if (!isWebhookConfigured(config.customBotUrl))
                return;
            String url = constructBotUrl(config.customBotUrl, "/api/flagged");

            JsonObject payload = new JsonObject();
            payload.addProperty("type", "flagged");
            payload.addProperty("username", username);
            payload.addProperty("content", content);
            payload.addProperty("mentionId", config.userMentionId);
            sendPayloadAsync(url, payload);
        } else {
            sendMessage(formatAlert("Flagged message", username, content));
        }
    }

    public void sendHandshake(String status, String username) {
        ModConfig config = ConfigManager.getConfig();
        if (config.useCustomBot) {
            if (!isWebhookConfigured(config.customBotUrl))
                return;
            String url = constructBotUrl(config.customBotUrl, "/api/handshake");

            JsonObject payload = new JsonObject();
            payload.addProperty("type", "handshake");
            payload.addProperty("status", status);
            payload.addProperty("username", username);
            sendPayloadAsync(url, payload);
        } else {
            // Fallback for standard webhook
            sendMessage(String.format("`Staff Handshake: %s %s`", username, status));
        }
    }

    private String constructBotUrl(String baseUrl, String endpoint) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpoint;
        }
        return baseUrl + endpoint;
    }

    private boolean isWebhookConfigured(String url) {
        return url != null && !url.isEmpty() && !url.contains("YOUR_WEBHOOK_URL_HERE");
    }

    private String appendMention(String message, ModConfig config) {
        if (config.enableDiscordPing && config.userMentionId != null && !config.userMentionId.isEmpty()) {
            return message + "\n<@" + config.userMentionId + ">";
        }
        return message;
    }

    private void sendPayloadAsync(String url, JsonObject payload) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    LOGGER.warn("Failed to send Discord payload. Status: {}", response.statusCode());
                }
            } catch (Exception e) {
                LOGGER.error("Error sending Discord payload", e);
            }
        });
    }

    public static String formatAlert(String alertType, String username, String content) {
        return String.format("`%s from %s: %s`", alertType, username, content);
    }

    public static String formatXRayAlert(String username, String count, String oreType) {
        return String.format("`X-Ray Alert: %s found x%s %s`", username, count, oreType);
    }

    public static String formatReportAlert(String reporter, String reportee, String reason) {
        return String.format("`Report: %s reported %s for %s`", reporter, reportee, reason);
    }
}
