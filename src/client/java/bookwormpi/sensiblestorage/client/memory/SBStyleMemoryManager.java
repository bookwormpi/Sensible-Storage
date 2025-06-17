package bookwormpi.sensiblestorage.client.memory;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * SB-Style client-only memory slot manager for containers
 */
public class SBStyleMemoryManager {
    private static final Map<String, ItemStack[]> containerMemory = new HashMap<>();
    private static final Map<String, Boolean> containerMemorizeMode = new HashMap<>();
    
    /**
     * Get or create memory templates for a container
     */
    public static ItemStack[] getMemoryTemplates(String containerId, int slotCount) {
        return containerMemory.computeIfAbsent(containerId, k -> {
            ItemStack[] templates = new ItemStack[slotCount];
            for (int i = 0; i < slotCount; i++) {
                templates[i] = ItemStack.EMPTY;
            }
            return templates;
        });
    }
    
    /**
     * SB-style memory capture: copy current items to memory templates
     */
    public static void captureMemory(String containerId, net.minecraft.screen.ScreenHandler handler) {
        int containerSlots = Math.max(0, handler.slots.size() - 36); // Exclude player inventory
        ItemStack[] templates = getMemoryTemplates(containerId, containerSlots);
        
        // Copy current container contents to memory (SB-style)
        for (int i = 0; i < containerSlots; i++) {
            ItemStack current = handler.getSlot(i).getStack();
            templates[i] = current.isEmpty() ? ItemStack.EMPTY : current.copy(); // SB-style shallow copy
        }
    }
    
    /**
     * Clear all memory templates for a container
     */
    public static void clearMemory(String containerId) {
        ItemStack[] templates = containerMemory.get(containerId);
        if (templates != null) {
            for (int i = 0; i < templates.length; i++) {
                templates[i] = ItemStack.EMPTY;
            }
        }
    }
    
    /**
     * Check if an item matches the memory template (SB-style matching)
     */
    public static boolean matchesTemplate(String containerId, int slotIndex, ItemStack stack) {
        ItemStack[] templates = containerMemory.get(containerId);
        if (templates == null || slotIndex >= templates.length || templates[slotIndex].isEmpty()) {
            return true; // No template means allow anything
        }
        
        // SB-style matching
        return ItemStack.areItemsAndComponentsEqual(templates[slotIndex], stack);
    }
    
    /**
     * Get memory template for a specific slot
     */
    public static ItemStack getTemplate(String containerId, int slotIndex) {
        ItemStack[] templates = containerMemory.get(containerId);
        if (templates == null || slotIndex < 0 || slotIndex >= templates.length) {
            return ItemStack.EMPTY;
        }
        return templates[slotIndex];
    }
    
    /**
     * Remove container memory when closed
     */
    public static void removeContainer(String containerId) {
        containerMemory.remove(containerId);
        containerMemorizeMode.remove(containerId);
    }
    
    /**
     * Toggle memorize mode for a container
     */
    public static boolean toggleMemorizeMode(String containerId) {
        boolean currentMode = containerMemorizeMode.getOrDefault(containerId, false);
        boolean newMode = !currentMode;
        containerMemorizeMode.put(containerId, newMode);
        return newMode;
    }
    
    /**
     * Check if container is in memorize mode
     */
    public static boolean isMemorizeMode(String containerId) {
        return containerMemorizeMode.getOrDefault(containerId, false);
    }
    
    /**
     * Set memory template for a specific slot (used in memorize mode)
     */
    public static void setTemplate(String containerId, int slotIndex, ItemStack stack) {
        ItemStack[] templates = containerMemory.get(containerId);
        if (templates != null && slotIndex >= 0 && slotIndex < templates.length) {
            templates[slotIndex] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
    }
}
