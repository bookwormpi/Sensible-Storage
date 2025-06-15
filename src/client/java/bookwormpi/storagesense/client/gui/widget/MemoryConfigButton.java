package bookwormpi.storagesense.client.gui.widget;

import bookwormpi.storagesense.client.gui.MemoryConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A button widget for opening the memory configuration screen
 * Styled to match container GUI aesthetics
 */
public class MemoryConfigButton extends ButtonWidget {
    private static final int BUTTON_SIZE = 16;
    
    public MemoryConfigButton(int x, int y, World world, BlockPos containerPos, Screen parentScreen) {
        super(x, y, BUTTON_SIZE, BUTTON_SIZE, Text.empty(), 
              button -> openMemoryConfig(world, containerPos, parentScreen), 
              DEFAULT_NARRATION_SUPPLIER);
        
        // Set tooltip
        this.setTooltip(Tooltip.of(Text.translatable("storagesense.gui.memory_button.tooltip")));
    }
    
    private static void openMemoryConfig(World world, BlockPos containerPos, Screen parentScreen) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new MemoryConfigScreen(world, containerPos, 0, parentScreen));
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw button background (simple colored rectangle for now)
        int color = this.isHovered() ? 0xFF5A5A5A : 0xFF404040;
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);
        
        // Draw border
        context.drawBorder(this.getX(), this.getY(), this.width, this.height, 0xFF8B8B8B);
        
        // Draw memory icon (simple "M" for now - we can improve this later)
        int textColor = this.isHovered() ? 0xFFFFFF : 0xC6C6C6;
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("M"),
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            textColor
        );
    }
}
