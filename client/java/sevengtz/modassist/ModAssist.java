//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sevengtz.autochatmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sevengtz.autochatmod.client.ActionMenuScreen;

public class AutoChatMod implements ClientModInitializer {
    public static final String MOD_ID = "autochatmod";
    public static final Logger LOGGER = LoggerFactory.getLogger("autochatmod");
    private static final Pattern MUTE_EVIDENCE_PATTERN = Pattern.compile("^\\[S] \\[([^\\]]+)\\] (\\w{2,}) was muted by (\\w{2,}) .* for \"(.*)\"\\.?$");
    private static ChatMonitor chatMonitor;
    public static final ActionMenuScreen ACTION_MENU = new ActionMenuScreen();
    private static final Identifier ACTION_MENU_ID = Identifier.of("autochatmod", "action_menu");
    private static final KeyBinding.Category AUTOCHAT_CATEGORY = Category.create(Identifier.of("autochatmod", "main"));
    private static KeyBinding keyBindTeleport;
    private static KeyBinding keyBindPunish;
    private static KeyBinding keyBindClose;
    private static KeyBinding keyBindSelectPlayer;
    private static KeyBinding keyBindCheckFly;
    private static KeyBinding keyBindAlts;

    public void onInitializeClient() {
        LOGGER.info("[ModAssist]: Initializing...");
        ConfigManager.init();
        chatMonitor = new ChatMonitor();
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, ACTION_MENU_ID, ACTION_MENU);
        ClientReceiveMessageEvents.MODIFY_GAME.register((ClientReceiveMessageEvents.ModifyGame)(message, overlay) -> {
            Text modifiedMessage = chatMonitor.makeMessageClickable(message);
            chatMonitor.processMessage(message.getString());
            return modifiedMessage;
        });
        ClientReceiveMessageEvents.GAME.register((ClientReceiveMessageEvents.Game)(message, overlay) -> {
            String text = message.getString();
            ConfigManager.Config config = ConfigManager.getConfig();
            LOGGER.info("[ModAssist]: Processing message: {}", text);
            LOGGER.info("[ModAssist]: Evidence enabled: {}, Moderator name: '{}'", config.evidenceScreenshotEnabled, config.evidenceModeratorName);
            if (config.evidenceScreenshotEnabled && config.evidenceModeratorName != null && !config.evidenceModeratorName.isEmpty()) {
                Matcher matcher = MUTE_EVIDENCE_PATTERN.matcher(text);
                if (matcher.find()) {
                    String serverName = matcher.group(1);
                    String mutedPlayer = matcher.group(2);
                    String moderator = matcher.group(3);
                    String reason = matcher.group(4);
                    LOGGER.info("[ModAssist]: Found mute message - Server: {}, Player: {}, Moderator: {}, Reason: {}", new Object[]{serverName, mutedPlayer, moderator, reason});
                    if (moderator.equalsIgnoreCase(config.evidenceModeratorName)) {
                        LOGGER.info("[ModAssist]: Taking screenshot for mute by configured moderator: {}", config.evidenceModeratorName);
                        ScreenshotEvidence.takeScreenshot(mutedPlayer, reason, MinecraftClient.getInstance());
                    } else {
                        LOGGER.debug("[ModAssist]: Moderator '{}' does not match configured moderator '{}'", moderator, config.evidenceModeratorName);
                    }
                } else {
                    LOGGER.info("[ModAssist]: Message does not match mute pattern");
                    LOGGER.info("[ModAssist]: Pattern is: {}", MUTE_EVIDENCE_PATTERN.pattern());
                }

            } else {
                LOGGER.debug("[ModAssist]: Evidence screenshot disabled or no moderator name set");
            }
        });
        LOGGER.info("[ModAssist]: Initialized successfully!");
        this.registerCommands();
        this.registerKeybinds();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal("autochatmod").then(ClientCommandManager.literal("action").then(ClientCommandManager.argument("username", StringArgumentType.word()).executes((context) -> {
            String username = StringArgumentType.getString(context, "username");
            LOGGER.info("[ModAssist]: Opening GUI for ", username);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                LOGGER.warn("[ModAssist]: Cannot open GUI - player is null");
                return 0;
            } else {
                client.execute(() -> {
                    if (client.currentScreen != null) {
                        client.setScreen((Screen)null);
                    }

                    LOGGER.debug("[ModAssist]: Calling show for {}", username);
                    ACTION_MENU.show(username, FlagType.MANUAL_CLICK);
                });
                return 1;
            }
        })))).then(ClientCommandManager.literal("testscreenshot").executes((context) -> {
            LOGGER.info("[ModAssist]: Testing screenshot functionality");
            MinecraftClient client = MinecraftClient.getInstance();
            ScreenshotEvidence.takeScreenshot("TestPlayer", "Test Reason", client);
            return 1;
        }))));
    }

    private void registerKeybinds() {
        keyBindTeleport = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.autochatmod.teleport", Type.KEYSYM, 88, AUTOCHAT_CATEGORY));
        keyBindPunish = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.autochatmod.punish", Type.KEYSYM, 80, AUTOCHAT_CATEGORY));
        keyBindClose = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.autochatmod.close", Type.KEYSYM, 67, AUTOCHAT_CATEGORY));
        keyBindAlts = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.autochatmod.alts", Type.KEYSYM, 76, AUTOCHAT_CATEGORY));
        keyBindCheckFly = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.autochatmod.checkfly", Type.KEYSYM, 72, AUTOCHAT_CATEGORY));
        keyBindSelectPlayer = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.autochatmod.select_player", Type.KEYSYM, 71, AUTOCHAT_CATEGORY));
        KeyBindApproveReport = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.autochatmod.approve_report", Type.KEYSYM, 57, AUTOCHAT_CATEGORY ))
        ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> {
            if (keyBindSelectPlayer.wasPressed() && client != null && client.crosshairTarget != null && client.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult)client.crosshairTarget;
                Entity targetEntity = entityHit.getEntity();
                if (targetEntity instanceof PlayerEntity) {
                    String targetUsername = ((PlayerEntity)targetEntity).getGameProfile().name();
                    LOGGER.info("[ModAssist]: Selected player {} via keybind.", targetUsername);
                    ACTION_MENU.show(targetUsername, FlagType.MANUAL_SELECT);
                    return;
                }
            }

            if (ACTION_MENU.isVisible() && client.player != null) {
                String currentUsername = ACTION_MENU.getUsername();
                if (currentUsername != null) {
                    if (keyBindTeleport.wasPressed()) {
                        LOGGER.info("[ModAssist]: Teleport keybind (X) pressed for {}", currentUsername);
                        client.player.networkHandler.sendChatCommand("tp " + currentUsername);
                        MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY).append(Text.literal("ModAssist").formatted(Formatting.YELLOW)).append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                        MutableText message = Text.literal("Teleporting to " + currentUsername).formatted(Formatting.GRAY);
                        client.player.sendMessage(prefix.append(message), false);
                    }

                    if (keyBindCheckFly.wasPressed()) {
                        LOGGER.info("[ModAssist]: Checking if user {} has /fly on", currentUsername);
                        client.player.networkHandler.sendChatCommand("checkfly " + currentUsername);
                        MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY).append(Text.literal("ModAssist").formatted(Formatting.YELLOW)).append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                        MutableText message = Text.literal("Checking " + currentUsername + " for /fly!").formatted(Formatting.GRAY);
                        client.player.sendMessage(prefix.append(message), false);
                    }

                    if (keyBindAlts.wasPressed()) {
                        LOGGER.info("[ModAssist]: Checking user's alts", currentUsername);
                        client.player.networkHandler.sendChatCommand("alts " + currentUsername + " true");
                        MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY).append(Text.literal("ModAssist").formatted(Formatting.YELLOW)).append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                        MutableText message = Text.literal("Checking " + currentUsername + "'s alts").formatted(Formatting.GRAY);
                        client.player.sendMessage(prefix.append(message), false);
                    }

                    if (keyBindPunish.wasPressed()) {
                        LOGGER.info("[ModAssist]: Punish keybind (P) pressed for {}", currentUsername);
                        MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY).append(Text.literal("ModAssist").formatted(Formatting.YELLOW)).append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                        boolean isInstantPunishCase = ACTION_MENU.getFlagType() == FlagType.SPAM && ConfigManager.getConfig().instantPunishForSpam;
                        if (isInstantPunishCase) {
                            client.player.networkHandler.sendChatCommand("punish " + currentUsername + " i:1");
                            MutableText message = Text.literal("Instantly punishing " + currentUsername + " for spam.").formatted(Formatting.GRAY);
                            client.player.sendMessage(prefix.append(message), false);
                            ACTION_MENU.hide();
                        } else {
                            client.player.networkHandler.sendChatCommand("punish " + currentUsername);
                            MutableText message = Text.literal("Opening punishment GUI for " + currentUsername).formatted(Formatting.GRAY);
                            client.player.sendMessage(prefix.append(message), false);
                        }
                    }

                    if (keyBindApproveReport.wasPressed()) {
                        LOGGER.info("[ModAssist]: Approving report", currentUsername);
                        client.player.networkHandler.sendChatCommand("approvereport " + currentUsername);
                        MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY).append(Text.literal("ModAssist").formatted(Formatting.YELLOW)).append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                        MutableText message = Text.literal("Approving report!").formatted(Formatting.GRAY);
                        client.player.sendMessage(prefix.append(message), false);
                    }

                    if (keyBindClose.wasPressed()) {
                        LOGGER.info("[ModAssist]: Close keybind (C) pressed");
                        MutableText prefix = Text.literal("[").formatted(Formatting.DARK_GRAY).append(Text.literal("ModAssist").formatted(Formatting.YELLOW)).append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
                        MutableText message = Text.literal("HUD closed").formatted(Formatting.GRAY);
                        client.player.sendMessage(prefix.append(message), false);
                        ACTION_MENU.hide();
                    }

                }
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

    public static KeyBinding getKeyBindApproveReport( {
        return keyBindApproveReport;
    })

    public static ChatMonitor getChatMonitor() {
        return chatMonitor;
    }
}
