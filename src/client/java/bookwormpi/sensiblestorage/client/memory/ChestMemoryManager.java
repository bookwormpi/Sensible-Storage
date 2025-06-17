package bookwormpi.sensiblestorage.client.memory;

import net.minecraft.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side memory slot manager following Sophisticated Backpacks' approach.
 * Provides SB-style memory templates for container slots with no server sync.
 */
public class ChestMemoryManager {
    // Client-only storage - per container ID
    private static final Map<Integer, ItemStack[]> containerMemoryMap = new HashMap<>();
    
    /**
     * Initialize memory templates for a container (SB-style)
     */
    public static void initializeMemory(int containerId, int slotCount) {
        ItemStack[] memoryTemplates = new ItemStack[slotCount];
        for (int i = 0; i < memoryTemplates.length; i++) {
            memoryTemplates[i] = ItemStack.EMPTY;
        }
        containerMemoryMap.put(containerId, memoryTemplates);
    }
    
    /**
     * Copy current container contents to memory templates (SB approach)
     */
    public static void captureMemory(int containerId, ItemStack[] containerSlots) {
        ItemStack[] templates = containerMemoryMap.get(containerId);
        if (templates == null || containerSlots == null) return;
        
        // SB-style shallow copy of current items
        int maxSlots = Math.min(templates.length, containerSlots.length);
        for (int i = 0; i < maxSlots; i++) {
            templates[i] = containerSlots[i].copy(); // SB uses ItemStack.copy()
        }
    }
    
    /**
     * Clear all memory templates (SB-style)
     */
    public static void clearMemory(int containerId) {
        ItemStack[] templates = containerMemoryMap.get(containerId);
        if (templates == null) return;
        
        for (int i = 0; i < templates.length; i++) {
            templates[i] = ItemStack.EMPTY;
        }
    }
    
    /**
     * Get memory template for a specific slot
     */
    public static ItemStack getMemoryTemplate(int containerId, int slotIndex) {
        ItemStack[] templates = containerMemoryMap.get(containerId);
        if (templates == null || slotIndex >= templates.length || slotIndex < 0) {
            return ItemStack.EMPTY;
        }
        return templates[slotIndex];
    }
    
    /**
     * Check if slot has a memory template
     */
    public static boolean hasMemoryTemplate(int containerId, int slotIndex) {
        return !getMemoryTemplate(containerId, slotIndex).isEmpty();
    }
    
    /**
     * Check if an item matches the memory template (SB-style matching)
     */
    public static boolean matchesTemplate(int containerId, int slotIndex, ItemStack stack) {
        ItemStack template = getMemoryTemplate(containerId, slotIndex);
        if (template.isEmpty()) return true; // No template = allow anything
        
        // SB uses ItemStack.matches() or similar
        return ItemStack.areItemsAndComponentsEqual(template, stack);
    }
    
    /**
     * Remove memory data when container closes (SB approach)
     */
    public static void onContainerClosed(int containerId) {
        containerMemoryMap.remove(containerId);
    }
    
    /**
     * Check if memory system is initialized for container
     */
    public static boolean isInitialized(int containerId) {
        return containerMemoryMap.containsKey(containerId);
    }
}
