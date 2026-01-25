package sevengtz.autochatmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.client.ActionMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.option.KeyBinding.Category;

public class AutoChatMod implements ClientModInitializer {
    public static final String MOD_ID = "autochatmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Pattern MUTE_EVIDENCE_PATTERN = Pattern
            .compile("^\\[S] \\[([^\\]]+)\\] (\\w{2,}) was muted by (\\w{2,}) .* for \"(.*)\"\\.?$");

    private static ChatMonitor chatMonitor;

    // Create a single instance of the ActionMenuScreen
    public static final ActionMenuScreen ACTION_MENU = new ActionMenuScreen();
    private static final Identifier ACTION_MENU_ID = Identifier.of(MOD_ID, "action_menu");
    private static final Category AUTOCHAT_CATEGORY = Category.create(Identifier.of("autochatmod", "main"));

    private static KeyBinding keyBindTeleport;
    private static KeyBinding keyBindPunish;
    private static KeyBinding keyBindClose;
    private static KeyBinding keyBindSelectPlayer;
    private static KeyBinding keyBindCheckFly;
    private static KeyBinding keyBindAlts;
    private static KeyBinding keyBindApproveReport;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AutoChatMod]: Initializing...");

        ConfigManager.init();
        chatMonitor = new ChatMonitor();

        // Register the HUD element with the registry
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, ACTION_MENU_ID, ACTION_MENU);

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            Text modifiedMessage = chatMonitor.makeMessageClickable(message);
            chatMonitor.processMessage(message.getString());
            return modifiedMessage;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            ConfigManager.Config config = ConfigManager.getConfig();

            LOGGER.info("[ModAssist]: Processing message: {}", text);
            LOGGER.info("[ModAssist]: Evidence enabled: {}",
                    config.evidenceScreenshotEnabled, MinecraftClient.getInstance().getName());

            if (!config.evidenceScreenshotEnabled || MinecraftClient.getInstance().getName() == null
                    || MinecraftClient.getInstance().getName().isEmpty()) {
                LOGGER.debug("[ModAssist]: Evidence screenshot disabled");
                return;
            }

            Matcher matcher = MUTE_EVIDENCE_PATTERN.matcher(text);
            if (matcher.find()) {
                String serverName = matcher.group(1);
                String mutedPlayer = matcher.group(2);
                String moderator = matcher.group(3);
                String reason = matcher.group(4);

                LOGGER.info("[ModAssist]: Found mute message - Server: {}, Player: {}, Moderator: {}, Reason: {}",
                        serverName, mutedPlayer, moderator, reason);

                if (moderator.equalsIgnoreCase(MinecraftClient.getInstance().getName())) {
                    LOGGER.info("[ModAssist]: Taking screenshot for mute by configured moderator: {}",
                            MinecraftClient.getInstance().getName());
                    ScreenshotEvidence.takeScreenshot(mutedPlayer, reason, MinecraftClient.getInstance());
                } else {
                    LOGGER.debug("[ModAssist]: Moderator '{}' does not match configured moderator '{}'",
                            moderator, MinecraftClient.getInstance().getName());
                }
            } else {
                // This part is for debugging and can be removed if you want
                LOGGER.info("[ModAssist]: Message does not match mute pattern");
                LOGGER.info("[ModAssist]: Pattern is: {}", MUTE_EVIDENCE_PATTERN.pattern());
            }
        });
        LOGGER.info("[ModAssist]: Initialized successfully!");
        registerCommands();
        registerKeybinds();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("autochatmod")
                    .then(ClientCommandManager.literal("action")
                            .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                    .executes(context -> {
                                        String username = StringArgumentType.getString(context, "username");
                                        LOGGER.info("[ModAssist]: Executing /autochatmod action for username: {}",
                                                username);
                                        MinecraftClient client = MinecraftClient.getInstance();
                                        if (client.player == null) {
                                            LOGGER.warn("[ModAssist]: Cannot open HUD - player is null");
                                            return 0;
                                        }
                                        client.execute(() -> {
                                            if (client.currentScreen != null) {
                                                client.setScreen(null);
                                            }
                                            LOGGER.debug("[ModAssist]: Calling show for {}", username);
                                            // Updated to call the instance method
                                            ACTION_MENU.show(username, FlagType.MANUAL_CLICK);
                                        });
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("testscreenshot")
                            .executes(context -> {
                                LOGGER.info("[ModAssist]: Testing screenshot functionality");
                                MinecraftClient client = MinecraftClient.getInstance();
                                ScreenshotEvidence.takeScreenshot("TestPlayer", "Test Reason", client);
                                return 1;
                            })));
        });
    }

    private void registerKeybinds() {
        // Assign default keys
        keyBindTeleport = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.teleport",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                AUTOCHAT_CATEGORY));

        keyBindPunish = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.punish",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                AUTOCHAT_CATEGORY));

        keyBindClose = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.close",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                AUTOCHAT_CATEGORY));

        keyBindAlts = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.alts",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                AUTOCHAT_CATEGORY));

        keyBindCheckFly = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.checkfly",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                AUTOCHAT_CATEGORY));

        keyBindSelectPlayer = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.select_player",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                AUTOCHAT_CATEGORY));
        keyBindApproveReport = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod.approve_report",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_9,
                AUTOCHAT_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (keyBindSelectPlayer.wasPressed()) {
                // Check if the client and crosshair target are valid
                if (client != null && client.crosshairTarget != null
                        && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
                    Entity targetEntity = entityHit.getEntity();

                    // Check if that entity is a player
                    if (targetEntity instanceof PlayerEntity) {
                        String targetUsername = ((PlayerEntity) targetEntity).getGameProfile().name();
                        LOGGER.info("[ModAssist]: Selected player {} via keybind.", targetUsername);
                        ACTION_MENU.show(targetUsername, FlagType.MANUAL_SELECT);
                        return;
                    }
                }
            }

            if (!ACTION_MENU.isVisible() || client.player == null) {
                return;
            }

            String currentUsername = ACTION_MENU.getUsername();
            if (currentUsername == null) {
                return;
            }

            if (keyBindTeleport.wasPressed()) {
                LOGGER.info("[ModAssist]: Teleport keybind (X) pressed for {}", currentUsername);

                boolean isOnline = false;
                if (client.getNetworkHandler() != null) {
                    isOnline = client.getNetworkHandler().getPlayerList().stream()
                            .anyMatch(entry -> entry.getProfile().name().equalsIgnoreCase(currentUsername));
                }

                String command = isOnline ? "tp " + currentUsername : "goto p:" + currentUsername;
                client.player.networkHandler.sendChatCommand(command);

                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("AutoChatMod").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text
                        .literal("Teleporting to " + currentUsername + (isOnline ? "" : " (Offline/Hidden)"))
                        .formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
            }

            if (keyBindCheckFly.wasPressed()) {
                LOGGER.info("[ModAssist]: Checking if user {} has /fly on", currentUsername);
                client.player.networkHandler.sendChatCommand("checkfly " + currentUsername);
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("ModAssist").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("Checking " + currentUsername + " for /fly!")
                        .formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
            }

            if (keyBindAlts.wasPressed()) {
                LOGGER.info("[ModAssist]: Checking if user {} has /fly on", currentUsername);
                client.player.networkHandler.sendChatCommand("alts " + currentUsername + " true");
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("ModAssist").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("Checking " + currentUsername + "'s alts")
                        .formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
            }

            if (keyBindPunish.wasPressed()) {
                LOGGER.info("[ModAssist]: Punish keybind (P) pressed for {}", currentUsername);

                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("ModAssist").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));

                boolean isInstantPunishCase = ACTION_MENU.getFlagType() == FlagType.SPAM
                        && ConfigManager.getConfig().instantPunishForSpam;

                if (isInstantPunishCase) {
                    client.player.networkHandler.sendChatCommand("punish " + currentUsername + " i:1");
                    MutableText message = Text.literal("Instantly punishing " + currentUsername + " for spam.")
                            .formatted(Formatting.GRAY);
                    client.player.sendMessage(prefix.append(message), false);
                    ACTION_MENU.hide();
                } else {
                    client.player.networkHandler.sendChatCommand("punish " + currentUsername);
                    MutableText message = Text.literal("Opening punishment GUI for " + currentUsername)
                            .formatted(Formatting.GRAY);
                    client.player.sendMessage(prefix.append(message), false);
                }
            }

            if (keyBindApproveReport.wasPressed()) {
                LOGGER.info("[ModAssist]: Approving Report!");
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("ModAssist").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("Report approved.").formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
                client.player.networkHandler.sendChatCommand("approvereport " + currentUsername);
            }

            if (keyBindClose.wasPressed()) {
                LOGGER.info("[ModAssist]: Close keybind (C) pressed");
                MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("ModAssist").formatted(Formatting.YELLOW))
                        .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                MutableText message = Text.literal("GUI closed.").formatted(Formatting.GRAY);
                client.player.sendMessage(prefix.append(message), false);
                ACTION_MENU.hide();
            }
        });
    }

    public static KeyBinding getKeyBindTeleport() {
        return keyBindTeleport;
    }

    public static KeyBinding getKeyBindPunish() {
        return keyBindPunish;
    }

    public static KeyBinding getKeyBindClose() {
        return keyBindClose;
    }

    public static KeyBinding getKeyBindCheckFly() {
        return keyBindCheckFly;
    }

    public static KeyBinding getKeyBindSelectPlayer() {
        return keyBindSelectPlayer;
    }

    public static KeyBinding getKeyBindAlts() {
        return keyBindAlts;
    }

    public static KeyBinding getKeyBindApproveReport() {
        return keyBindApproveReport;
    }

    public static ChatMonitor getChatMonitor() {
        return chatMonitor;
    }

}