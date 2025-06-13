package bookwormpi.storagesense.client.memory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * SB-style memory slot renderer for ghost items and visual indicators
 */
public class ChestMemoryRenderer {
    
    /**
     * Render ghost item in slot (SB-style translucent effect)
     */
    public static void renderGhostItem(DrawContext context, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        
        // SB-style: Render item with overlay to make it appear ghostly
        context.getMatrices().push();
        
        // Draw the item normally first
        context.drawItem(stack, x, y);
        
        // Draw translucent overlay for ghost effect (like SB)
        context.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        
        context.getMatrices().pop();
    }
    
    /**
     * Render memory slot tooltip (SB-style)
     */
    public static void renderMemoryTooltip(DrawContext context, ItemStack template, int mouseX, int mouseY) {
        if (template.isEmpty()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            context.drawTooltip(
                client.textRenderer,
                net.minecraft.text.Text.literal("Only ")
                    .append(template.getName())
                    .append(" can go here (client memory)"),
                mouseX, mouseY
            );
        }
    }
    
    /**
     * Render subtle border for slots with memory templates (SB-style)
     */
    public static void renderMemoryBorder(DrawContext context, int x, int y) {
        // SB-style subtle border indication
        int borderColor = 0x80FFFF00; // Translucent yellow
        
        // Draw border lines
        context.fill(x - 1, y - 1, x + 17, y, borderColor); // Top
        context.fill(x - 1, y + 16, x + 17, y + 17, borderColor); // Bottom
        context.fill(x - 1, y, x, y + 16, borderColor); // Left
        context.fill(x + 16, y, x + 17, y + 16, borderColor); // Right
    }
}
