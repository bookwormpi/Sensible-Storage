package bookwormpi.sensiblestorage.client.gui.widget;

import bookwormpi.sensiblestorage.client.memory.EnhancedSBStyleMemoryManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A SophisticatedBackpacks-style memory button with brain icon
 * Mimics the visual style of SB/SC buttons with proper texture rendering
 */
public class SBStyleMemoryButton extends ButtonWidget {
    
    // Button background colors (SB-style)
    private static final int INACTIVE_BACKGROUND = 0xFF_555555; // Gray background
    private static final int ACTIVE_BACKGROUND = 0xFF_4A8B3A;   // Green background
    private static final int HOVERED_INACTIVE = 0xFF_777777;    // Lighter gray when hovered
    private static final int HOVERED_ACTIVE = 0xFF_5FA848;      // Lighter green when hovered
    
    private final World world;
    private final BlockPos containerPos;
    private final HandledScreen<?> parentScreen;
    
    public SBStyleMemoryButton(int x, int y, World world, BlockPos containerPos, HandledScreen<?> parentScreen) {
        super(x, y, 16, 16, Text.translatable("sensiblestorage.gui.memory_mode_toggle"), 
              button -> {}, DEFAULT_NARRATION_SUPPLIER);
        
        this.world = world;
        this.containerPos = containerPos;
        this.parentScreen = parentScreen;
    }
    
    @Override
    public void onPress() {
        // Toggle memorization mode for this container
        String containerId = EnhancedSBStyleMemoryManager.createContainerId(
            parentScreen.getClass().getSimpleName(), 
            parentScreen.getScreenHandler().syncId, 
            containerPos
        );
        
        boolean newMode = EnhancedSBStyleMemoryManager.toggleMemorizeMode(containerId);
        
        // Optional: Play a sound or provide feedback
        // MinecraftClient.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    /**
     * Check if memorization mode is active for this container
     */
    private boolean isMemoryActive() {
        // Create container ID and check if memorization mode is enabled
        String containerId = EnhancedSBStyleMemoryManager.createContainerId(
            parentScreen.getClass().getSimpleName(), 
            parentScreen.getScreenHandler().syncId, 
            containerPos
        );
        return EnhancedSBStyleMemoryManager.isMemorizeMode(containerId);
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isActive = isMemoryActive();
        boolean isHovered = isHovered();
        
        // Determine background color based on state
        int backgroundColor;
        if (isActive) {
            backgroundColor = isHovered ? HOVERED_ACTIVE : ACTIVE_BACKGROUND;
        } else {
            backgroundColor = isHovered ? HOVERED_INACTIVE : INACTIVE_BACKGROUND;
        }
        
        // Render background (SB-style button background)
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), backgroundColor);
        
        // Render border (subtle darker border)
        int borderColor = isActive ? 0xFF_2D5A1F : 0xFF_333333;
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), borderColor);
        
        // Render brain icon
        renderBrainIcon(context);
    }
    
    /**
     * Render a simple brain-like icon using text
     */
    private void renderBrainIcon(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Use a brain-like symbol or fallback to "M" for Memory
        String iconChar = "🧠"; // Brain emoji
        
        // Center the icon in the button
        int textWidth = client.textRenderer.getWidth(iconChar);
        int textX = getX() + (getWidth() - textWidth) / 2;
        int textY = getY() + (getHeight() - client.textRenderer.fontHeight) / 2;
        
        // White text for good contrast
        context.drawText(client.textRenderer, iconChar, textX, textY, 0xFFFFFFFF, false);
    }
    
    @Override
    public void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE, 
                   Text.translatable("sensiblestorage.gui.memory_mode_toggle.narration"));
    }
}
