//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sevengtz.autochatmod;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();

    public void sendMessage(String message) {
        ConfigManager.Config config = ConfigManager.getConfig();
        if (!config.webhookUrl.isEmpty() && !config.webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL_HERE")) {
            String finalMessage = message;
            if (config.enableDiscordPing && !config.userMentionId.equals("YOUR ID HERE")) {
                finalMessage = message + "\n<@" + config.userMentionId + ">";
            }

            this.sendMessageAsync(finalMessage);
        } else {
            AutoChatMod.LOGGER.warn("Discord webhook URL not configured");
        }
    }

    private void sendMessageAsync(String message) {
        CompletableFuture.runAsync(() -> {
            try {
                ConfigManager.Config config = ConfigManager.getConfig();
                JsonObject payload = new JsonObject();
                payload.addProperty("content", message);
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(config.webhookUrl)).header("Content-Type", "application/json").POST(BodyPublishers.ofString(payload.toString())).build();
                HttpResponse<String> response = this.httpClient.send(request, BodyHandlers.ofString());
                if (response.statusCode() != 200 && response.statusCode() != 204) {
                    AutoChatMod.LOGGER.warn("Failed to send Discord message. Status: {}", response.statusCode());
                }
            } catch (InterruptedException | IOException e) {
                AutoChatMod.LOGGER.error("Error sending Discord message", e);
            }

        });
    }
}
