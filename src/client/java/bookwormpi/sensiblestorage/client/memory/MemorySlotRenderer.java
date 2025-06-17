package bookwormpi.sensiblestorage.client.memory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * Handles rendering of ghost items for memory slots, following SB's approach
 */
public class MemorySlotRenderer {
    
    /**
     * Render a ghost item in a slot (SB-style translucent rendering)
     */
    public static void renderGhostItem(DrawContext context, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        
        // Save current matrices state
        context.getMatrices().push();
        
        // Render the item with reduced opacity (ghost effect like SB)
        context.drawItem(stack, x, y);
        
        // Draw a translucent overlay to make it look ghostly
        context.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        
        // Restore matrices state
        context.getMatrices().pop();
    }
    
    /**
     * Render memory slot indicator (like SB's slot overlays)
     */
    public static void renderMemoryIndicator(DrawContext context, int x, int y) {
        // Draw a subtle border to indicate this slot has a memory template
        context.fill(x - 1, y - 1, x + 17, y, 0x80FFFFFF); // Top
        context.fill(x - 1, y + 16, x + 17, y + 17, 0x80FFFFFF); // Bottom  
        context.fill(x - 1, y, x, y + 16, 0x80FFFFFF); // Left
        context.fill(x + 16, y, x + 17, y + 16, 0x80FFFFFF); // Right
    }
    
    /**
     * Render tooltip for memory slot functionality
     */
    public static void renderMemoryTooltip(DrawContext context, ItemStack template, int mouseX, int mouseY) {
        if (template.isEmpty()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            context.drawTooltip(
                client.textRenderer,
                java.util.List.of(
                    net.minecraft.text.Text.literal("Memory Slot: Only ").append(template.getName()).append(" can go here")
                ),
                mouseX, mouseY
            );
        }
    }
}
