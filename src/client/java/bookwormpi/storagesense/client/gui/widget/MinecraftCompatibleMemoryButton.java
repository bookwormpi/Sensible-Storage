package bookwormpi.storagesense.client.gui.widget;

import bookwormpi.storagesense.client.gui.MemoryConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A Minecraft-compatible button widget for opening memory configuration
 * This extends ButtonWidget to ensure proper compatibility with Minecraft's screen system
 */
public class MinecraftCompatibleMemoryButton extends ButtonWidget {
    
    private final World world;
    private final BlockPos containerPos;
    private final HandledScreen<?> parentScreen;
    
    public MinecraftCompatibleMemoryButton(int x, int y, World world, BlockPos containerPos, HandledScreen<?> parentScreen) {
        super(x, y, 16, 16, Text.literal("M"), button -> {
            // Button click will be handled by the instance method
        }, DEFAULT_NARRATION_SUPPLIER);
        
        this.world = world;
        this.containerPos = containerPos;
        this.parentScreen = parentScreen;
    }
    
    @Override
    public void onPress() {
        
        // Open memory config for slot 0 as default
        MemoryConfigScreen memoryScreen = new MemoryConfigScreen(
            world,
            containerPos,
            0,
            parentScreen
        );
        
        MinecraftClient.getInstance().setScreen(memoryScreen);
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Simple background
        context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 
                     isHovered() ? 0xFFFFFFFF : 0xFF888888);
        
        // Border
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), 0xFF000000);
        
        // "M" text
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, 
                                          Text.literal("M"), 
                                          getX() + getWidth() / 2, 
                                          getY() + getHeight() / 2 - 4, 
                                          0xFF000000);
    }
}
