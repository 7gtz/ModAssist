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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.config.ConfigManager;
import sevengtz.autochatmod.config.ModConfig;
import sevengtz.autochatmod.integration.ScreenshotService;
import sevengtz.autochatmod.model.FlagType;
import sevengtz.autochatmod.service.ChatMonitorService;
import sevengtz.autochatmod.ui.ActionMenuScreen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main entry point for the AutoChatMod client-side mod.
 * Handles initialization, event registration, and keybind management.
 */
public class AutoChatMod implements ClientModInitializer {

    public static final String MOD_ID = "autochatmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Pattern MUTE_EVIDENCE_PATTERN = Pattern.compile(
            "^\\[S] \\[([^\\]]+)\\] (\\w{2,}) was muted by (\\w{2,}) .* for \"(.*)\"\\?$");
    private static final Identifier ACTION_MENU_ID = Identifier.of(MOD_ID, "action_menu");
    private static final KeyBinding.Category AUTOCHAT_CATEGORY = KeyBinding.Category.create(
            Identifier.of(MOD_ID, "keybinds"));

    // Services
    private static ChatMonitorService chatMonitor;
    private static ActionMenuScreen actionMenu;

    // Keybindings
    private static KeyBinding keyBindCommand1;
    private static KeyBinding keyBindCommand2;
    private static KeyBinding keyBindCommand3;
    private static KeyBinding keyBindCommand4;
    private static KeyBinding keyBindClose;
    private static KeyBinding keyBindSelectPlayer;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[AutoChatMod]: Initializing...");

        // Initialize configuration
        ConfigManager.init();

        // Initialize services
        chatMonitor = new ChatMonitorService();
        actionMenu = new ActionMenuScreen();

        // Register HUD element
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, ACTION_MENU_ID, actionMenu);

        // Register message event handlers
        registerMessageHandlers();

        // Register commands
        registerCommands();

        // Register keybindings
        registerKeybinds();

        LOGGER.info("[AutoChatMod]: Initialized successfully!");
    }

    /**
     * Registers chat message event handlers.
     */
    private void registerMessageHandlers() {
        // Modify game messages to make them clickable
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            Text modifiedMessage = chatMonitor.makeMessageClickable(message);
            chatMonitor.processMessage(message.getString());
            return modifiedMessage;
        });

        // Handle evidence screenshots
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleEvidenceScreenshot(message.getString());
        });
    }

    /**
     * Handles automatic evidence screenshots when mutes occur.
     */
    private void handleEvidenceScreenshot(String text) {
        ModConfig config = ConfigManager.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();

        if (!config.evidenceScreenshotEnabled ||
                client.getName() == null ||
                client.getName().isEmpty()) {
            return;
        }

        Matcher matcher = MUTE_EVIDENCE_PATTERN.matcher(text);
        if (matcher.find()) {
            String mutedPlayer = matcher.group(2);
            String moderator = matcher.group(3);
            String reason = matcher.group(4);

            LOGGER.info("[AutoChatMod]: Found mute message - Player: {}, Moderator: {}, Reason: {}",
                    mutedPlayer, moderator, reason);

            if (moderator.equalsIgnoreCase(client.getName())) {
                LOGGER.info("[AutoChatMod]: Taking screenshot for mute by: {}", client.getName());
                ScreenshotService.takeScreenshot(mutedPlayer, reason, client);
            }
        }
    }

    /**
     * Registers client-side commands.
     */
    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("autochatmod")
                    .then(ClientCommandManager.literal("action")
                            .then(ClientCommandManager.argument("username", StringArgumentType.word())
                                    .executes(context -> {
                                        String username = StringArgumentType.getString(context, "username");
                                        LOGGER.info("[AutoChatMod]: Executing /autochatmod action for: {}", username);

                                        MinecraftClient client = MinecraftClient.getInstance();
                                        if (client.player == null) {
                                            LOGGER.warn("[AutoChatMod]: Cannot open HUD - player is null");
                                            return 0;
                                        }

                                        client.execute(() -> {
                                            if (client.currentScreen != null) {
                                                client.setScreen(null);
                                            }
                                            actionMenu.show(username, FlagType.MANUAL_CLICK);
                                        });
                                        return 1;
                                    })))
                    .then(ClientCommandManager.literal("testscreenshot")
                            .executes(context -> {
                                LOGGER.info("[AutoChatMod]: Testing screenshot functionality");
                                MinecraftClient client = MinecraftClient.getInstance();
                                ScreenshotService.takeScreenshot("TestPlayer", "Test Reason", client);
                                return 1;
                            })));
        });
    }

    /**
     * Registers keybindings and their handlers.
     */
    private void registerKeybinds() {
        // Register all keybindings
        keyBindCommand1 = registerKey("command_1", GLFW.GLFW_KEY_P);
        keyBindCommand2 = registerKey("command_2", GLFW.GLFW_KEY_L);
        keyBindCommand3 = registerKey("command_3", GLFW.GLFW_KEY_H);
        keyBindCommand4 = registerKey("command_4", GLFW.GLFW_KEY_9);
        keyBindClose = registerKey("close", GLFW.GLFW_KEY_C);
        keyBindSelectPlayer = registerKey("select_player", GLFW.GLFW_KEY_G);

        // Register tick handler for keybind processing
        ClientTickEvents.END_CLIENT_TICK.register(this::handleKeybinds);
    }

    /**
     * Helper method to register a keybinding.
     */
    private KeyBinding registerKey(String name, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autochatmod." + name,
                InputUtil.Type.KEYSYM,
                defaultKey,
                AUTOCHAT_CATEGORY));
    }

    /**
     * Handles keybind presses each tick.
     */
    private void handleKeybinds(MinecraftClient client) {
        // Handle player selection keybind (works when overlay is not visible)
        if (keyBindSelectPlayer.wasPressed()) {
            handleSelectPlayer(client);
        }

        // All other keybinds require overlay to be visible
        if (!actionMenu.isVisible() || client.player == null) {
            return;
        }

        String username = actionMenu.getUsername();
        if (username == null)
            return;

        if (keyBindCommand1.wasPressed()) {
            handleCommand1(client, username);
        }

        if (keyBindCommand2.wasPressed()) {
            handleCommand2(client, username);
        }

        if (keyBindCommand3.wasPressed()) {
            handleCommand3(client, username);
        }

        if (keyBindCommand4.wasPressed()) {
            handleCommand4(client, username);
        }

        if (keyBindClose.wasPressed()) {
            handleClose(client);
        }
    }

    private void handleSelectPlayer(MinecraftClient client) {
        if (client.crosshairTarget != null &&
                client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
            Entity targetEntity = entityHit.getEntity();

            if (targetEntity instanceof PlayerEntity) {
                String targetUsername = ((PlayerEntity) targetEntity).getGameProfile().name();
                LOGGER.info("[AutoChatMod]: Selected player {} via keybind.", targetUsername);
                actionMenu.show(targetUsername, FlagType.MANUAL_SELECT);
            }
        }
    }

    private void handleCommand1(MinecraftClient client, String username) {
        LOGGER.info("[AutoChatMod]: Command #1 keybind pressed for {}", username);
        ModConfig config = ConfigManager.getConfig();

        boolean isInstantAction = actionMenu.getFlagType() == FlagType.SPAM &&
                config.instantPunishForSpam;

        String commandTemplate = isInstantAction ? config.command1InstantCommand : config.command1Command;
        String command = ModConfig.processCommand(commandTemplate, username);
        client.player.networkHandler.sendChatCommand(command);

        if (isInstantAction) {
            sendFeedback(client, "Instant action on " + username + " for spam.");
            actionMenu.hide();
        } else {
            sendFeedback(client, "Running: /" + command);
        }
    }

    private void handleCommand2(MinecraftClient client, String username) {
        LOGGER.info("[AutoChatMod]: Command #2 keybind pressed for {}", username);
        ModConfig config = ConfigManager.getConfig();
        String command = ModConfig.processCommand(config.command2Command, username);
        client.player.networkHandler.sendChatCommand(command);
        sendFeedback(client, "Running: /" + command);
    }

    private void handleCommand3(MinecraftClient client, String username) {
        LOGGER.info("[AutoChatMod]: Command #3 keybind pressed for {}", username);
        ModConfig config = ConfigManager.getConfig();
        String command = ModConfig.processCommand(config.command3Command, username);
        client.player.networkHandler.sendChatCommand(command);
        sendFeedback(client, "Running: /" + command);
    }

    private void handleCommand4(MinecraftClient client, String username) {
        LOGGER.info("[AutoChatMod]: Command #4 keybind pressed for {}", username);
        ModConfig config = ConfigManager.getConfig();
        String command = ModConfig.processCommand(config.command4Command, username);
        client.player.networkHandler.sendChatCommand(command);
        sendFeedback(client, "Running: /" + command);
    }

    private void handleClose(MinecraftClient client) {
        LOGGER.info("[AutoChatMod]: Close keybind pressed");
        sendFeedback(client, "GUI closed.");
        actionMenu.hide();
    }

    private void sendFeedback(MinecraftClient client, String message) {
        if (client.player == null)
            return;

        // Premium styled feedback: ✦ ModAssist › message
        MutableText icon = Text.literal("✦ ")
                .setStyle(Style.EMPTY.withColor(0x6366F1)); // Indigo accent

        MutableText brand = Text.literal("ModAssist")
                .setStyle(Style.EMPTY.withColor(0x6366F1).withBold(true));

        MutableText separator = Text.literal(" › ")
                .setStyle(Style.EMPTY.withColor(0x555555));

        MutableText content = Text.literal(message)
                .setStyle(Style.EMPTY.withColor(0xE2E8F0)); // Light gray

        MutableText fullMessage = icon.append(brand).append(separator).append(content);
        client.player.sendMessage(fullMessage, false);
    }

    // Public accessors for keybindings (used by ActionMenuScreen)
    public static KeyBinding getKeyBindCommand1() {
        return keyBindCommand1;
    }

    public static KeyBinding getKeyBindCommand2() {
        return keyBindCommand2;
    }

    public static KeyBinding getKeyBindCommand3() {
        return keyBindCommand3;
    }

    public static KeyBinding getKeyBindCommand4() {
        return keyBindCommand4;
    }

    public static KeyBinding getKeyBindClose() {
        return keyBindClose;
    }

    public static KeyBinding getKeyBindSelectPlayer() {
        return keyBindSelectPlayer;
    }

    // Service accessors
    public static ChatMonitorService getChatMonitor() {
        return chatMonitor;
    }

    public static ActionMenuScreen getActionMenu() {
        return actionMenu;
    }
}