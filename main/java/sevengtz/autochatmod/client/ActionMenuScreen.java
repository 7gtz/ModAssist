package sevengtz.autochatmod.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext; // Correct import for modern rendering
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import sevengtz.autochatmod.FlagType;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.Formatting;
import net.minecraft.client.option.KeyBinding;

import sevengtz.autochatmod.AutoChatMod;

public class ActionMenuScreen implements HudElement {

    private static boolean isOverlayVisible = false;
    private static String username = null;
    private static FlagType currentFlagType = FlagType.MANUAL_CLICK;

    // Dragging functionality
    private boolean isDragging = false;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private boolean isResizing = false;
    private double resizeStartX = 0;
    private double resizeStartY = 0;

    private int hudX = -1;
    private int hudY = -1;
    private boolean wasMousePressed = false;

    // HUD dimensions
    private int hudWidth = 250;
    private int hudHeight = 100;
    private static final int PADDING = 5;

    // Method to show the overlay
    public void show(String user, FlagType type) {
        username = user;
        currentFlagType = type;
        isOverlayVisible = true;

        sevengtz.autochatmod.ConfigManager.Config config = sevengtz.autochatmod.ConfigManager.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        this.hudWidth = config.hudWidth;
        this.hudHeight = config.hudHeight;

        // Initialization of position can be done here or in the constructor
        if (hudX == -1) {

            int screenWidth = client.getWindow().getScaledWidth();
            hudX = screenWidth - hudWidth - PADDING;
            hudY = PADDING;
        }
    }

    // Method to hide the overlay
    public void hide() {
        isOverlayVisible = false;
        username = null;
        currentFlagType = FlagType.MANUAL_CLICK;
        isDragging = false;
    }

    public boolean isVisible() {
        return isOverlayVisible;
    }

    public String getUsername() {
        return username;
    }

    public FlagType getFlagType() {
        return currentFlagType;
    }

    @Override
    public void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        // Exit if the overlay shouldn't be visible
        if (!isOverlayVisible || username == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Handle all mouse input for dragging and resizing
        handleMouseInput(client);

        // Ensure the HUD stays within the screen bounds
        this.hudX = Math.max(0, Math.min(this.hudX, screenWidth - this.hudWidth));
        this.hudY = Math.max(0, Math.min(this.hudY, screenHeight - this.hudHeight));

        // Draw the main background (changes color when moving/resizing)
        int backgroundColor = (isDragging || isResizing) ? ColorHelper.getArgb(200, 50, 50, 100)
                : ColorHelper.getArgb(180, 0, 0, 0);
        drawContext.fill(this.hudX, this.hudY, this.hudX + this.hudWidth, this.hudY + this.hudHeight, backgroundColor);

        // Highlight the resize zone on hover
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        int hotZoneSize = 10;
        boolean mouseOverResizeZone = !isDragging && mouseX >= this.hudX + this.hudWidth - hotZoneSize
                && mouseX <= this.hudX + this.hudWidth &&
                mouseY >= this.hudY + this.hudHeight - hotZoneSize && mouseY <= this.hudY + this.hudHeight;
        if (mouseOverResizeZone) {
            int highlightColor = ColorHelper.getArgb(100, 255, 255, 255); // Semi-transparent white
            drawContext.fill(
                    this.hudX + this.hudWidth - hotZoneSize,
                    this.hudY + this.hudHeight - hotZoneSize,
                    this.hudX + this.hudWidth,
                    this.hudY + this.hudHeight,
                    highlightColor);
        }

        // Draw the title bar and text
        int titleBarHeight = 15;
        drawContext.fill(this.hudX, this.hudY, this.hudX + this.hudWidth, this.hudY + titleBarHeight,
                ColorHelper.getArgb(220, 100, 100, 100));
        drawContext.drawText(textRenderer, "Actions for: " + username, this.hudX + PADDING, this.hudY + PADDING,
                0xFFFFFFFF, true);

        // Dynamically draw actions that have a key bound
        int yOffset = this.hudY + PADDING + 20;
        int lineHeight = 15;

        // Action 1: Teleport
        KeyBinding teleportBinding = AutoChatMod.getKeyBindTeleport();
        if (!teleportBinding.isUnbound()) {
            String key = teleportBinding.getBoundKeyLocalizedText().getString();
            Text text = Text.literal("Teleport to User (")
                    .append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
            drawContext.drawText(textRenderer, text, this.hudX + PADDING, yOffset, 0xFF00FF00, true); // Green
            yOffset += lineHeight;
        }

        // Action 2: Punish
        KeyBinding punishBinding = AutoChatMod.getKeyBindPunish();
        if (!punishBinding.isUnbound()) {
            String key = punishBinding.getBoundKeyLocalizedText().getString();
            Text text = Text.literal("Punish User (")
                    .append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
            drawContext.drawText(textRenderer, text, this.hudX + PADDING, yOffset, 0xFFFF0000, true); // Red
            yOffset += lineHeight;
        }

        // Action 3: Alts
        KeyBinding altsBinding = AutoChatMod.getKeyBindAlts();
        if (!altsBinding.isUnbound()) {
            String key = altsBinding.getBoundKeyLocalizedText().getString();
            Text text = Text.literal("Check Alts (")
                    .append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
            drawContext.drawText(textRenderer, text, this.hudX + PADDING, yOffset, 0xFFFFFFFF, true); // White
            yOffset += lineHeight;
        }

        // Action 4: CheckFly
        KeyBinding checkflyBinding = AutoChatMod.getKeyBindCheckFly();
        if (!checkflyBinding.isUnbound()) {
            String key = checkflyBinding.getBoundKeyLocalizedText().getString();
            Text text = Text.literal("Run /checkfly (")
                    .append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
            drawContext.drawText(textRenderer, text, this.hudX + PADDING, yOffset, 0xFFFFFFFF, true); // White
            yOffset += lineHeight;
        }

        // Action 5: Approve Report (Only if REPORT)
        if (currentFlagType == FlagType.REPORT) {
            KeyBinding approveReportBinding = AutoChatMod.getKeyBindApproveReport();
            if (!approveReportBinding.isUnbound()) {
                String key = approveReportBinding.getBoundKeyLocalizedText().getString();
                Text text = Text.literal("Approve Report (")
                        .append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
                drawContext.drawText(textRenderer, text, this.hudX + PADDING, yOffset, 0xFF55FF55, true); // Green
                yOffset += lineHeight;
            }
        }

        // Draw dragging indicator text if dragging
        if (isDragging) {
            drawContext.drawText(textRenderer, "Dragging...", this.hudX + PADDING, this.hudY + this.hudHeight - 15,
                    0xFFFFFF00, true);
        }
    }

    private void handleMouseInput(MinecraftClient client) {
        if (client.mouse == null)
            return;
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        boolean mousePressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        // Define a "hot zone" for resizing (e.g., the bottom-right corner)
        int hotZoneSize = 10;
        boolean mouseOverResizeZone = mouseX >= this.hudX + this.hudWidth - hotZoneSize
                && mouseX <= this.hudX + this.hudWidth &&
                mouseY >= this.hudY + this.hudHeight - hotZoneSize && mouseY <= this.hudY + this.hudHeight;

        boolean mouseOverHUD = mouseX >= this.hudX && mouseX <= this.hudX + this.hudWidth &&
                mouseY >= this.hudY && mouseY <= this.hudY + this.hudHeight;

        // When the mouse button is released, stop all actions and save
        if (!mousePressed && (isDragging || isResizing)) {
            isDragging = false;
            isResizing = false;
            // Save the final position and size to the config
            sevengtz.autochatmod.ConfigManager.Config config = sevengtz.autochatmod.ConfigManager.getConfig();
            config.hudX = this.hudX;
            config.hudY = this.hudY;
            config.hudWidth = this.hudWidth;
            config.hudHeight = this.hudHeight;
            sevengtz.autochatmod.ConfigManager.saveConfig();
        }

        // Start a resize if clicking in the hot zone
        if (mousePressed && !wasMousePressed && mouseOverResizeZone) {
            isResizing = true;
            isDragging = false; // Ensure we are not dragging at the same time
            resizeStartX = mouseX;
            resizeStartY = mouseY;
        }
        // Start a drag if clicking anywhere else on the HUD
        else if (mousePressed && !wasMousePressed && mouseOverHUD) {
            isDragging = true;
            isResizing = false;
            dragStartX = mouseX - this.hudX;
            dragStartY = mouseY - this.hudY;
        }

        // Update dimensions while resizing
        if (isResizing && mousePressed) {
            int minWidth = 150;
            int minHeight = 75;
            this.hudWidth = (int) Math.max(minWidth, mouseX - this.hudX);
            this.hudHeight = (int) Math.max(minHeight, mouseY - this.hudY);
        }

        // Update position while dragging
        if (isDragging && mousePressed) {
            this.hudX = (int) (mouseX - dragStartX);
            this.hudY = (int) (mouseY - dragStartY);
        }

        wasMousePressed = mousePressed;
    }
}