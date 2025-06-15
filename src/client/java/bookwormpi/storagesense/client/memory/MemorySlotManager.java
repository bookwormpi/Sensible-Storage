package bookwormpi.storagesense.client.memory;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side memory slot manager following Sophisticated Backpacks approach.
 * Stores item templates for container slots to provide visual filtering guidance.
 */
public class MemorySlotManager {
    private static final Map<Integer, ItemStack[]> containerMemoryMap = new HashMap<>();
    
    /**
     * Initialize memory slots for a container
     */
    public static void initializeMemory(int containerId, int slotCount) {
        ItemStack[] memoryTemplates = new ItemStack[slotCount];
        for (int i = 0; i < slotCount; i++) {
            memoryTemplates[i] = ItemStack.EMPTY;
        }
        containerMemoryMap.put(containerId, memoryTemplates);
    }
    
    /**
     * Copy current container contents to memory templates (SB-style)
     */
    public static void captureMemory(int containerId, ScreenHandler handler) {
        ItemStack[] templates = containerMemoryMap.get(containerId);
        if (templates == null) return;
        
        // Only capture non-player inventory slots (like SB does)
        for (int i = 0; i < templates.length; i++) {
            Slot slot = handler.getSlot(i);
            if (slot != null && !isPlayerInventorySlot(slot, handler)) {
                templates[i] = slot.getStack().copy(); // SB-style shallow copy
            }
        }
    }
    
    /**
     * Clear all memory templates for a container
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
        if (templates == null || slotIndex >= templates.length) {
            return ItemStack.EMPTY;
        }
        return templates[slotIndex];
    }
    
    /**
     * Check if an item matches the memory template for a slot
     */
    public static boolean matchesTemplate(int containerId, int slotIndex, ItemStack stack) {
        ItemStack template = getMemoryTemplate(containerId, slotIndex);
        if (template.isEmpty()) return true; // No template means allow anything
        
        // SB-style matching - same item type and components
        return ItemStack.areItemsAndComponentsEqual(template, stack);
    }
    
    /**
     * Check if a slot has a memory template
     */
    public static boolean hasTemplate(int containerId, int slotIndex) {
        return !getMemoryTemplate(containerId, slotIndex).isEmpty();
    }
    
    /**
     * Remove memory data when container closes
     */
    public static void onContainerClosed(int containerId) {
        containerMemoryMap.remove(containerId);
    }
    
    /**
     * Check if a slot belongs to player inventory (to exclude from memory capture)
     */
    private static boolean isPlayerInventorySlot(Slot slot, ScreenHandler handler) {
        // Player inventory slots are typically the last 36 slots (hotbar + main inventory)
        int totalSlots = handler.slots.size();
        int slotIndex = handler.slots.indexOf(slot);
        return slotIndex >= totalSlots - 36;
    }
}
