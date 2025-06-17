package bookwormpi.sensiblestorage.client.memory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced SB-Style memory manager with optional persistent storage
 * Can work in both session-only and persistent modes
 */
public class EnhancedSBStyleMemoryManager {
    private static final Map<String, ItemStack[]> sessionMemory = new HashMap<>();
    private static final Map<String, Boolean> sessionMemorizeMode = new HashMap<>();
    
    // Configuration
    private static boolean usePersistentStorage = true;
    private static boolean isInitialized = false;
    
    /**
     * Initialize the enhanced memory manager
     */
    public static void init() {
        if (isInitialized) return;
        
        if (usePersistentStorage) {
            PersistentMemoryManager.init();
        }
        
        isInitialized = true;
    }
    
    /**
     * Create a container ID - tries to create a persistent ID if possible, falls back to session ID
     */
    public static String createContainerId(String screenClassName, int syncId, BlockPos containerPos) {
        if (usePersistentStorage && containerPos != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                String dimension = client.world.getRegistryKey().getValue().toString();
                String containerType = extractContainerType(screenClassName);
                return PersistentMemoryManager.createContainerIdFromPosition(
                    dimension, containerPos.getX(), containerPos.getY(), containerPos.getZ(), containerType);
            }
        }
        
        // Fallback to session-based ID
        return screenClassName + "_" + syncId;
    }
    
    /**
     * Extract container type from screen class name
     */
    private static String extractContainerType(String screenClassName) {
        // Extract meaningful container type from class name
        String simplified = screenClassName.toLowerCase();
        if (simplified.contains("chest")) return "chest";
        if (simplified.contains("shulker")) return "shulker_box";
        if (simplified.contains("barrel")) return "barrel";
        if (simplified.contains("hopper")) return "hopper";
        if (simplified.contains("dispenser")) return "dispenser";
        if (simplified.contains("dropper")) return "dropper";
        if (simplified.contains("furnace")) return "furnace";
        return "container";
    }
    
    /**
     * Get or create memory templates for a container
     */
    public static ItemStack[] getMemoryTemplates(String containerId, int slotCount) {
        if (usePersistentStorage) {
            return PersistentMemoryManager.getMemoryTemplates(containerId, slotCount);
        } else {
            return sessionMemory.computeIfAbsent(containerId, k -> {
                ItemStack[] templates = new ItemStack[slotCount];
                for (int i = 0; i < slotCount; i++) {
                    templates[i] = ItemStack.EMPTY;
                }
                return templates;
            });
        }
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
            templates[i] = current.isEmpty() ? ItemStack.EMPTY : current.copy();
        }
        
        // Mark as modified for persistent storage
        if (usePersistentStorage) {
            PersistentMemoryManager.markModified(containerId);
            // Auto-save after capturing memory
            PersistentMemoryManager.save();
        }
        
    }
    
    /**
     * Clear all memory templates for a container
     */
    public static void clearMemory(String containerId) {
        if (usePersistentStorage) {
            // Clear from persistent storage
            ItemStack[] templates = PersistentMemoryManager.getMemoryTemplates(containerId, 0);
            if (templates != null) {
                for (int i = 0; i < templates.length; i++) {
                    templates[i] = ItemStack.EMPTY;
                }
                PersistentMemoryManager.markModified(containerId);
                PersistentMemoryManager.save();
            }
        } else {
            // Clear from session storage
            ItemStack[] templates = sessionMemory.get(containerId);
            if (templates != null) {
                for (int i = 0; i < templates.length; i++) {
                    templates[i] = ItemStack.EMPTY;
                }
            }
        }
        
    }
    
    /**
     * Check if an item matches the memory template (SB-style matching)
     */
    public static boolean matchesTemplate(String containerId, int slotIndex, ItemStack stack) {
        // We need the total container size, not slotIndex + 1
        ItemStack[] templates = usePersistentStorage ? 
            PersistentMemoryManager.getMemoryTemplates(containerId, Math.max(54, slotIndex + 1)) :
            sessionMemory.get(containerId);
            
        if (templates == null || slotIndex >= templates.length || templates[slotIndex].isEmpty()) {
            return true; // No template means allow anything
        }
        
        // SB-style matching
        return ItemStack.areItemsAndComponentsEqual(templates[slotIndex], stack);
    }
    
    /**
     * Check if an item matches a template (static utility method)
     * Used for slot filtering
     */
    public static boolean itemMatchesTemplate(ItemStack template, ItemStack item) {
        if (template.isEmpty()) {
            return true; // No template means allow anything
        }
        
        if (item.isEmpty()) {
            return false; // Can't place empty item if template exists
        }
        
        // SB-style matching - same item type and components, ignore count
        return ItemStack.areItemsAndComponentsEqual(template, item);
    }
     /**
     * Get memory template for a specific slot
     */
    public static ItemStack getTemplate(String containerId, int slotIndex) {
        // We need the total container size, not slotIndex + 1
        // For persistent storage, we'll use a reasonable default and let it resize if needed
        ItemStack[] templates = usePersistentStorage ? 
            PersistentMemoryManager.getMemoryTemplates(containerId, Math.max(54, slotIndex + 1)) :
            sessionMemory.get(containerId);
            
        if (templates == null || slotIndex < 0 || slotIndex >= templates.length) {
            return ItemStack.EMPTY;
        }

        ItemStack result = templates[slotIndex];
        return result;
    }
    
    /**
     * Get memory template for a specific slot with known container size
     */
    public static ItemStack getTemplate(String containerId, int slotIndex, int containerSlots) {
        ItemStack[] templates = usePersistentStorage ? 
            PersistentMemoryManager.getMemoryTemplates(containerId, containerSlots) :
            sessionMemory.get(containerId);
            
        if (templates == null || slotIndex < 0 || slotIndex >= templates.length) {
            return ItemStack.EMPTY;
        }

        ItemStack result = templates[slotIndex];
        return result;
    }
    
    /**
     * Set memory template for a specific slot (used in memorize mode)
     */
    public static void setTemplate(String containerId, int slotIndex, ItemStack stack) {
        // We need the total container size, not slotIndex + 1
        // For persistent storage, we'll use a reasonable default and let it resize if needed
        ItemStack[] templates = usePersistentStorage ? 
            PersistentMemoryManager.getMemoryTemplates(containerId, Math.max(54, slotIndex + 1)) :
            sessionMemory.get(containerId);
            
        if (templates != null && slotIndex >= 0 && slotIndex < templates.length) {
            templates[slotIndex] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            
            if (usePersistentStorage) {
                PersistentMemoryManager.markModified(containerId);
                // Auto-save when setting individual templates
                PersistentMemoryManager.save();
            }
            
        }
    }
    
    /**
     * Set memory template for a specific slot with known container size (used in memorize mode)
     */
    public static void setTemplate(String containerId, int slotIndex, ItemStack stack, int containerSlots) {
        ItemStack[] templates = usePersistentStorage ? 
            PersistentMemoryManager.getMemoryTemplates(containerId, containerSlots) :
            sessionMemory.get(containerId);
            
        if (templates != null && slotIndex >= 0 && slotIndex < templates.length) {
            templates[slotIndex] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            
            if (usePersistentStorage) {
                PersistentMemoryManager.markModified(containerId);
                // Auto-save when setting individual templates
                PersistentMemoryManager.save();
            }
            
        }
    }
    
    /**
     * Toggle memorize mode for a container
     */
    public static boolean toggleMemorizeMode(String containerId) {
        if (usePersistentStorage) {
            PersistentMemoryManager.ContainerMemory memory = PersistentMemoryManager.getOrCreateMemory(containerId, 0);
            memory.memorizeMode = !memory.memorizeMode;
            PersistentMemoryManager.markModified(containerId);
            return memory.memorizeMode;
        } else {
            boolean currentMode = sessionMemorizeMode.getOrDefault(containerId, false);
            boolean newMode = !currentMode;
            sessionMemorizeMode.put(containerId, newMode);
            return newMode;
        }
    }
    
    /**
     * Check if container is in memorize mode
     */
    public static boolean isMemorizeMode(String containerId) {
        if (usePersistentStorage) {
            PersistentMemoryManager.ContainerMemory memory = PersistentMemoryManager.getOrCreateMemory(containerId, 0);
            return memory.memorizeMode;
        } else {
            return sessionMemorizeMode.getOrDefault(containerId, false);
        }
    }
    
    /**
     * Remove container memory when closed (session mode only - persistent mode keeps data)
     */
    public static void removeContainer(String containerId) {
        if (usePersistentStorage) {
            // In persistent mode, we don't remove on close - data persists between sessions
            // Reset memorize mode though
            PersistentMemoryManager.ContainerMemory memory = PersistentMemoryManager.getOrCreateMemory(containerId, 0);
            memory.memorizeMode = false;
        } else {
            // In session mode, remove completely
            sessionMemory.remove(containerId);
            sessionMemorizeMode.remove(containerId);
        }
    }
    
    /**
     * Force remove a container (even in persistent mode)
     */
    public static void forceRemoveContainer(String containerId) {
        if (usePersistentStorage) {
            PersistentMemoryManager.removeContainer(containerId);
        } else {
            sessionMemory.remove(containerId);
            sessionMemorizeMode.remove(containerId);
        }
    }
    
    /**
     * Save all data (persistent mode only)
     */
    public static void saveAll() {
        if (usePersistentStorage) {
            PersistentMemoryManager.save();
        }
    }
    
    /**
     * Get statistics about loaded containers
     */
    public static String getStats() {
        if (usePersistentStorage) {
            return String.format("Persistent mode: %d containers loaded", 
                PersistentMemoryManager.getLoadedContainerIds().size());
        } else {
            return String.format("Session mode: %d containers in memory", sessionMemory.size());
        }
    }
    
    /**
     * Migrate memory templates between different container IDs
     */
    public static void migrateMemory(String fromContainerId, String toContainerId) {
        if (fromContainerId.equals(toContainerId)) return;
        
        ItemStack[] fromTemplates = usePersistentStorage ? 
            PersistentMemoryManager.getMemoryTemplates(fromContainerId, 0) :
            sessionMemory.get(fromContainerId);
            
        if (fromTemplates == null || fromTemplates.length == 0) return;
        
        // Get or create templates for destination
        ItemStack[] toTemplates = getMemoryTemplates(toContainerId, fromTemplates.length);
        
        // Copy templates
        System.arraycopy(fromTemplates, 0, toTemplates, 0, Math.min(fromTemplates.length, toTemplates.length));
        
        // Copy memorize mode state
        boolean memorizeMode = usePersistentStorage ?
            PersistentMemoryManager.getOrCreateMemory(fromContainerId, 0).memorizeMode :
            sessionMemorizeMode.getOrDefault(fromContainerId, false);
            
        if (usePersistentStorage) {
            PersistentMemoryManager.getOrCreateMemory(toContainerId, 0).memorizeMode = memorizeMode;
            PersistentMemoryManager.markModified(toContainerId);
        } else {
            sessionMemorizeMode.put(toContainerId, memorizeMode);
        }
        
    }
    
    /**
     * Check if any memory templates exist for a container
     */
    public static boolean hasAnyMemoryTemplates(String containerId) {
        init();
        
        if (usePersistentStorage) {
            return PersistentMemoryManager.hasAnyMemoryTemplates(containerId);
        } else {
            ItemStack[] templates = sessionMemory.get(containerId);
            if (templates == null) return false;
            
            for (ItemStack template : templates) {
                if (template != null && !template.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
}
