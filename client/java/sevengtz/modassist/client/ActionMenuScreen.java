//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sevengtz.autochatmod.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;
import sevengtz.autochatmod.AutoChatMod;
import sevengtz.autochatmod.ConfigManager;
import sevengtz.autochatmod.FlagType;

public class ActionMenuScreen implements HudElement {
    private static boolean isOverlayVisible = false;
    private static String username = null;
    private static FlagType currentFlagType;
    private boolean isDragging = false;
    private double dragStartX = (double)0.0F;
    private double dragStartY = (double)0.0F;
    private boolean isResizing = false;
    private double resizeStartX = (double)0.0F;
    private double resizeStartY = (double)0.0F;
    private int hudX = -1;
    private int hudY = -1;
    private boolean wasMousePressed = false;
    private int hudWidth = 250;
    private int hudHeight = 100;
    private static final int PADDING = 5;

    public void show(String user, FlagType type) {
        username = user;
        currentFlagType = type;
        isOverlayVisible = true;
        ConfigManager.Config config = ConfigManager.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        this.hudWidth = config.hudWidth;
        this.hudHeight = config.hudHeight;
        if (this.hudX == -1) {
            int screenWidth = client.getWindow().getScaledWidth();
            this.hudX = screenWidth - this.hudWidth - 5;
            this.hudY = 5;
        }

    }

    public void hide() {
        isOverlayVisible = false;
        username = null;
        currentFlagType = FlagType.MANUAL_CLICK;
        this.isDragging = false;
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

    public void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (isOverlayVisible && username != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer textRenderer = client.textRenderer;
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            this.handleMouseInput(client);
            this.hudX = Math.max(0, Math.min(this.hudX, screenWidth - this.hudWidth));
            this.hudY = Math.max(0, Math.min(this.hudY, screenHeight - this.hudHeight));
            int backgroundColor = !this.isDragging && !this.isResizing ? ColorHelper.getArgb(180, 0, 0, 0) : ColorHelper.getArgb(200, 50, 50, 100);
            drawContext.fill(this.hudX, this.hudY, this.hudX + this.hudWidth, this.hudY + this.hudHeight, backgroundColor);
            double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
            int hotZoneSize = 10;
            boolean mouseOverResizeZone = !this.isDragging && mouseX >= (double)(this.hudX + this.hudWidth - hotZoneSize) && mouseX <= (double)(this.hudX + this.hudWidth) && mouseY >= (double)(this.hudY + this.hudHeight - hotZoneSize) && mouseY <= (double)(this.hudY + this.hudHeight);
            if (mouseOverResizeZone) {
                int highlightColor = ColorHelper.getArgb(100, 255, 255, 255);
                drawContext.fill(this.hudX + this.hudWidth - hotZoneSize, this.hudY + this.hudHeight - hotZoneSize, this.hudX + this.hudWidth, this.hudY + this.hudHeight, highlightColor);
            }

            int titleBarHeight = 15;
            drawContext.fill(this.hudX, this.hudY, this.hudX + this.hudWidth, this.hudY + titleBarHeight, ColorHelper.getArgb(220, 100, 100, 100));
            drawContext.drawText(textRenderer, "Actions for: " + username, this.hudX + 5, this.hudY + 5, -1, true);
            int yOffset = this.hudY + 5 + 20;
            int lineHeight = 15;
            KeyBinding teleportBinding = AutoChatMod.getKeyBindTeleport();
            if (!teleportBinding.isUnbound()) {
                String key = teleportBinding.getBoundKeyLocalizedText().getString();
                Text text = Text.literal("Teleport to User (").append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
                drawContext.drawText(textRenderer, text, this.hudX + 5, yOffset, -16711936, true);
                yOffset += lineHeight;
            }

            KeyBinding punishBinding = AutoChatMod.getKeyBindPunish();
            if (!punishBinding.isUnbound()) {
                String key = punishBinding.getBoundKeyLocalizedText().getString();
                Text text = Text.literal("Punish User (").append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
                drawContext.drawText(textRenderer, text, this.hudX + 5, yOffset, -65536, true);
                yOffset += lineHeight;
            }

            KeyBinding altsBinding = AutoChatMod.getKeyBindAlts();
            if (!altsBinding.isUnbound()) {
                String key = altsBinding.getBoundKeyLocalizedText().getString();
                Text text = Text.literal("Check Alts (").append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
                drawContext.drawText(textRenderer, text, this.hudX + 5, yOffset, -1, true);
                yOffset += lineHeight;
            }

            KeyBinding checkflyBinding = AutoChatMod.getKeyBindCheckFly();
            if (!checkflyBinding.isUnbound()) {
                String key = checkflyBinding.getBoundKeyLocalizedText().getString();
                Text text = Text.literal("Run /checkfly (").append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
                drawContext.drawText(textRenderer, text, this.hudX + 5, yOffset, -1, true);
            }

            KeyBinding checkApproveReport = AutoChatMod.getKeyBindApproveReport();
            if (!checkApproveReport.isUnbound()) {
                String key = checkApproveReport.getBoundKeyLocalizedText().getString();
                Text text = Text.literal("Approve Report (").append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.YELLOW))).append(")");
                drawContext.drawText(textRenderer, text, this.hudX + 5, yOffset, -1, true);
            }

            if (this.isDragging) {
                drawContext.drawText(textRenderer, "Dragging...", this.hudX + 5, this.hudY + this.hudHeight - 15, -256, true);
            }

        }
    }

    private void handleMouseInput(MinecraftClient client) {
        if (client.mouse != null) {
            double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
            boolean mousePressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), 0) == 1;
            int hotZoneSize = 10;
            boolean mouseOverResizeZone = mouseX >= (double)(this.hudX + this.hudWidth - hotZoneSize) && mouseX <= (double)(this.hudX + this.hudWidth) && mouseY >= (double)(this.hudY + this.hudHeight - hotZoneSize) && mouseY <= (double)(this.hudY + this.hudHeight);
            boolean mouseOverHUD = mouseX >= (double)this.hudX && mouseX <= (double)(this.hudX + this.hudWidth) && mouseY >= (double)this.hudY && mouseY <= (double)(this.hudY + this.hudHeight);
            if (!mousePressed && (this.isDragging || this.isResizing)) {
                this.isDragging = false;
                this.isResizing = false;
                ConfigManager.Config config = ConfigManager.getConfig();
                config.hudX = this.hudX;
                config.hudY = this.hudY;
                config.hudWidth = this.hudWidth;
                config.hudHeight = this.hudHeight;
                ConfigManager.saveConfig();
            }

            if (mousePressed && !this.wasMousePressed && mouseOverResizeZone) {
                this.isResizing = true;
                this.isDragging = false;
                this.resizeStartX = mouseX;
                this.resizeStartY = mouseY;
            } else if (mousePressed && !this.wasMousePressed && mouseOverHUD) {
                this.isDragging = true;
                this.isResizing = false;
                this.dragStartX = mouseX - (double)this.hudX;
                this.dragStartY = mouseY - (double)this.hudY;
            }

            if (this.isResizing && mousePressed) {
                int minWidth = 150;
                int minHeight = 75;
                this.hudWidth = (int)Math.max((double)minWidth, mouseX - (double)this.hudX);
                this.hudHeight = (int)Math.max((double)minHeight, mouseY - (double)this.hudY);
            }

            if (this.isDragging && mousePressed) {
                this.hudX = (int)(mouseX - this.dragStartX);
                this.hudY = (int)(mouseY - this.dragStartY);
            }

            this.wasMousePressed = mousePressed;
        }
    }

    static {
        currentFlagType = FlagType.MANUAL_CLICK;
    }
}
