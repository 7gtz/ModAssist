package sevengtz.autochatmod.ui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;
import sevengtz.autochatmod.AutoChatMod;
import sevengtz.autochatmod.config.ConfigManager;
import sevengtz.autochatmod.config.ModConfig;
import sevengtz.autochatmod.model.FlagType;

/**
 * Premium-styled HUD overlay that displays available actions for a targeted
 * player.
 * Supports dragging and resizing, with a modern glassmorphism-inspired design.
 */
public class ActionMenuScreen implements HudElement {

    // Layout constants
    private static final int PADDING = 8;
    private static final int TITLE_BAR_HEIGHT = 22;
    private static final int LINE_HEIGHT = 18;
    private static final int HOT_ZONE_SIZE = 12;
    private static final int MIN_WIDTH = 180;
    private static final int MIN_HEIGHT = 100;

    // Premium color palette
    private static final int COLOR_BG_PRIMARY = ColorHelper.getArgb(220, 18, 18, 24);
    private static final int COLOR_BG_TITLE = ColorHelper.getArgb(255, 30, 30, 40);
    private static final int COLOR_ACCENT = ColorHelper.getArgb(255, 99, 102, 241);
    private static final int COLOR_SUCCESS = ColorHelper.getArgb(255, 34, 197, 94);
    private static final int COLOR_DANGER = ColorHelper.getArgb(255, 239, 68, 68);
    private static final int COLOR_WARNING = ColorHelper.getArgb(255, 250, 204, 21);
    private static final int COLOR_TEXT_PRIMARY = ColorHelper.getArgb(255, 248, 250, 252);
    private static final int COLOR_TEXT_SECONDARY = ColorHelper.getArgb(255, 148, 163, 184);
    private static final int COLOR_BORDER = ColorHelper.getArgb(80, 99, 102, 241);
    private static final int COLOR_RESIZE_HOVER = ColorHelper.getArgb(150, 99, 102, 241);

    // State
    private boolean isVisible = false;
    private String username = null;
    private FlagType currentFlagType = FlagType.MANUAL_CLICK;

    // Dragging state
    private boolean isDragging = false;
    private double dragStartX = 0;
    private double dragStartY = 0;

    // Resizing state
    private boolean isResizing = false;

    // Position and size
    private int hudX = -1;
    private int hudY = -1;
    private int hudWidth = 260;
    private int hudHeight = 140;
    private boolean wasMousePressed = false;

    /**
     * Shows the action menu for a specific user.
     */
    public void show(String user, FlagType type) {
        this.username = user;
        this.currentFlagType = type;
        this.isVisible = true;

        ModConfig config = ConfigManager.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        this.hudWidth = config.hudWidth;
        this.hudHeight = config.hudHeight;

        if (hudX == -1) {
            int screenWidth = client.getWindow().getScaledWidth();
            hudX = screenWidth - hudWidth - PADDING;
            hudY = PADDING;
        }
    }

    /**
     * Hides the action menu.
     */
    public void hide() {
        this.isVisible = false;
        this.username = null;
        this.currentFlagType = FlagType.MANUAL_CLICK;
        this.isDragging = false;
        this.isResizing = false;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public String getUsername() {
        return username;
    }

    public FlagType getFlagType() {
        return currentFlagType;
    }

    @Override
    public void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!isVisible || username == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        handleMouseInput(client);
        constrainToScreen(screenWidth, screenHeight);

        drawPremiumBackground(drawContext);
        drawPremiumTitleBar(drawContext, textRenderer);
        drawPremiumActions(drawContext, textRenderer);
        drawResizeIndicator(drawContext, client);
    }

    /**
     * Draws a premium glassmorphism-style background with subtle border.
     */
    private void drawPremiumBackground(DrawContext drawContext) {
        // Outer glow/border
        drawContext.fill(hudX - 1, hudY - 1, hudX + hudWidth + 1, hudY + hudHeight + 1, COLOR_BORDER);

        // Main background
        int bgColor = (isDragging || isResizing)
                ? ColorHelper.getArgb(235, 30, 30, 45)
                : COLOR_BG_PRIMARY;
        drawContext.fill(hudX, hudY, hudX + hudWidth, hudY + hudHeight, bgColor);

        // Top accent line
        drawContext.fill(hudX, hudY, hudX + hudWidth, hudY + 2, COLOR_ACCENT);
    }

    /**
     * Draws the premium title bar with flag type indicator.
     */
    private void drawPremiumTitleBar(DrawContext drawContext, TextRenderer textRenderer) {
        // Title bar background
        drawContext.fill(hudX, hudY + 2, hudX + hudWidth, hudY + TITLE_BAR_HEIGHT, COLOR_BG_TITLE);

        // Flag type indicator dot
        int indicatorColor = getFlagTypeColor();
        int dotX = hudX + PADDING;
        int dotY = hudY + TITLE_BAR_HEIGHT / 2 - 2;
        drawContext.fill(dotX, dotY, dotX + 6, dotY + 6, indicatorColor);

        // Title text
        String title = truncateText(username, hudWidth - 40, textRenderer);
        drawContext.drawText(textRenderer, Text.literal(title).setStyle(Style.EMPTY.withBold(true)),
                hudX + PADDING + 10, hudY + 7, COLOR_TEXT_PRIMARY, true);

        // Flag type label on the right
        String flagLabel = getFlagTypeLabel();
        int labelWidth = textRenderer.getWidth(flagLabel);
        drawContext.drawText(textRenderer, flagLabel,
                hudX + hudWidth - labelWidth - PADDING, hudY + 7, COLOR_TEXT_SECONDARY, false);
    }

    private int getFlagTypeColor() {
        return switch (currentFlagType) {
            case SPAM -> COLOR_DANGER;
            case XRAY -> COLOR_WARNING;
            case REPORT -> COLOR_ACCENT;
            case FLAGGED_PHRASE -> COLOR_DANGER;
            default -> COLOR_SUCCESS;
        };
    }

    private String getFlagTypeLabel() {
        return switch (currentFlagType) {
            case SPAM -> "SPAM";
            case XRAY -> "X-RAY";
            case REPORT -> "REPORT";
            case FLAGGED_PHRASE -> "FLAGGED";
            case MANUAL_SELECT -> "SELECT";
            default -> "MANUAL";
        };
    }

    /**
     * Draws premium-styled action buttons.
     */
    private void drawPremiumActions(DrawContext drawContext, TextRenderer textRenderer) {
        ModConfig config = ConfigManager.getConfig();
        int yOffset = hudY + TITLE_BAR_HEIGHT + PADDING;

        yOffset = drawPremiumAction(drawContext, textRenderer, AutoChatMod.getKeyBindTeleport(),
                "Teleport to Player", COLOR_SUCCESS, yOffset);

        yOffset = drawPremiumAction(drawContext, textRenderer, AutoChatMod.getKeyBindPunish(),
                "Punish Player", COLOR_DANGER, yOffset);

        yOffset = drawPremiumAction(drawContext, textRenderer, AutoChatMod.getKeyBindCommand1(),
                config.command1Label, COLOR_TEXT_PRIMARY, yOffset);

        yOffset = drawPremiumAction(drawContext, textRenderer, AutoChatMod.getKeyBindCommand2(),
                config.command2Label, COLOR_TEXT_PRIMARY, yOffset);

        if (currentFlagType == FlagType.REPORT) {
            drawPremiumAction(drawContext, textRenderer, AutoChatMod.getKeyBindCommand3(),
                    config.command3Label, COLOR_ACCENT, yOffset);
        }
    }

    /**
     * Draws a single premium-styled action.
     */
    private int drawPremiumAction(DrawContext drawContext, TextRenderer textRenderer,
            KeyBinding binding, String label, int accentColor, int yOffset) {
        if (binding == null || binding.isUnbound()) {
            return yOffset;
        }

        String key = binding.getBoundKeyLocalizedText().getString();

        // Key badge background
        int keyWidth = textRenderer.getWidth(key) + 6;
        int badgeX = hudX + PADDING;
        int badgeY = yOffset - 1;
        drawContext.fill(badgeX, badgeY, badgeX + keyWidth, badgeY + LINE_HEIGHT - 2,
                ColorHelper.getArgb(100, 255, 255, 255));

        // Key text
        drawContext.drawText(textRenderer, key, badgeX + 3, yOffset + 2, COLOR_BG_PRIMARY, false);

        // Label with accent color for first character
        Text labelText = Text.literal(label).setStyle(Style.EMPTY.withColor(accentColor));
        drawContext.drawText(textRenderer, labelText, badgeX + keyWidth + 6, yOffset + 2, accentColor, false);

        return yOffset + LINE_HEIGHT;
    }

    /**
     * Draws the resize indicator in the corner.
     */
    private void drawResizeIndicator(DrawContext drawContext, MinecraftClient client) {
        int cornerX = hudX + hudWidth - HOT_ZONE_SIZE;
        int cornerY = hudY + hudHeight - HOT_ZONE_SIZE;

        // Draw corner lines
        int lineColor = isMouseOverResizeZone(client) ? COLOR_RESIZE_HOVER : COLOR_TEXT_SECONDARY;

        // Diagonal lines pattern
        for (int i = 0; i < 3; i++) {
            int offset = i * 3;
            drawContext.fill(cornerX + offset + 4, cornerY + HOT_ZONE_SIZE - 2,
                    cornerX + offset + 6, cornerY + HOT_ZONE_SIZE, lineColor);
        }

        // Status indicator when interacting
        if (isDragging) {
            drawContext.drawText(client.textRenderer, "✥ Moving",
                    hudX + PADDING, hudY + hudHeight - 14, COLOR_WARNING, true);
        } else if (isResizing) {
            drawContext.drawText(client.textRenderer, "⇲ Resizing",
                    hudX + PADDING, hudY + hudHeight - 14, COLOR_WARNING, true);
        }
    }

    private String truncateText(String text, int maxWidth, TextRenderer textRenderer) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        while (textRenderer.getWidth(text + "...") > maxWidth && text.length() > 1) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    // ========== Mouse Handling ==========

    private void constrainToScreen(int screenWidth, int screenHeight) {
        hudX = Math.max(0, Math.min(hudX, screenWidth - hudWidth));
        hudY = Math.max(0, Math.min(hudY, screenHeight - hudHeight));
    }

    private void handleMouseInput(MinecraftClient client) {
        if (client.mouse == null)
            return;

        double[] mousePos = getScaledMousePosition(client);
        double mouseX = mousePos[0];
        double mouseY = mousePos[1];
        boolean mousePressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (!mousePressed && (isDragging || isResizing)) {
            finishInteraction();
        }

        if (mousePressed && !wasMousePressed && isMouseOverResizeZone(client)) {
            startResizing();
        } else if (mousePressed && !wasMousePressed && isMouseOverHud(mouseX, mouseY)) {
            startDragging(mouseX, mouseY);
        }

        if (isResizing && mousePressed) {
            updateResize(mouseX, mouseY);
        }
        if (isDragging && mousePressed) {
            updateDrag(mouseX, mouseY);
        }

        wasMousePressed = mousePressed;
    }

    private double[] getScaledMousePosition(MinecraftClient client) {
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth()
                / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight()
                / client.getWindow().getHeight();
        return new double[] { mouseX, mouseY };
    }

    private boolean isMouseOverResizeZone(MinecraftClient client) {
        double[] mousePos = getScaledMousePosition(client);
        return mousePos[0] >= hudX + hudWidth - HOT_ZONE_SIZE &&
                mousePos[0] <= hudX + hudWidth &&
                mousePos[1] >= hudY + hudHeight - HOT_ZONE_SIZE &&
                mousePos[1] <= hudY + hudHeight;
    }

    private boolean isMouseOverHud(double mouseX, double mouseY) {
        return mouseX >= hudX && mouseX <= hudX + hudWidth &&
                mouseY >= hudY && mouseY <= hudY + hudHeight;
    }

    private void startDragging(double mouseX, double mouseY) {
        isDragging = true;
        isResizing = false;
        dragStartX = mouseX - hudX;
        dragStartY = mouseY - hudY;
    }

    private void startResizing() {
        isResizing = true;
        isDragging = false;
    }

    private void updateDrag(double mouseX, double mouseY) {
        hudX = (int) (mouseX - dragStartX);
        hudY = (int) (mouseY - dragStartY);
    }

    private void updateResize(double mouseX, double mouseY) {
        hudWidth = (int) Math.max(MIN_WIDTH, mouseX - hudX);
        hudHeight = (int) Math.max(MIN_HEIGHT, mouseY - hudY);
    }

    private void finishInteraction() {
        isDragging = false;
        isResizing = false;

        ModConfig config = ConfigManager.getConfig();
        config.hudX = hudX;
        config.hudY = hudY;
        config.hudWidth = hudWidth;
        config.hudHeight = hudHeight;
        ConfigManager.saveConfig();
    }
}
