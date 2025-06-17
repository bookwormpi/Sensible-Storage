package bookwormpi.storagesense.mixin;

import bookwormpi.storagesense.client.container.ContainerTracker;
import bookwormpi.storagesense.client.memory.EnhancedSBStyleMemoryManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept slot interactions and enforce memory configurations
 * Based on SophisticatedCore's approach but adapted for Fabric
 */
@Mixin(Slot.class)
public class SlotMixin {
    
    @Shadow @Final
    public Inventory inventory;
    
    @Shadow
    private int index;
    
    /**
     * Intercept canTakeItems to check memory configurations
     * This method is called for shift-click and other taking operations
     * NOTE: We should be MORE PERMISSIVE for taking operations - users should be able to 
     * remove items from slots even if they don't match templates (to fix mistakes)
     */
    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    public void storagesense$checkMemoryBeforeTakeItems(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        // CRITICAL: Early exit for ANY player inventory operations (main inventory + hotbar)
        if (isPlayerInventorySlot()) {
            return;
        }
        
        // For taking operations, we should be MORE PERMISSIVE
        // Users should generally be able to take items out of containers
        // even if they don't match templates (to fix mistakes)
        // Only the insertion logic should be restrictive
        
        // Let vanilla logic handle taking operations - don't block them
        // The main filtering should happen in canInsert, not canTakeItems
        return;
    }
    
    /**
     * Intercept canInsert to check memory configurations  
     * This is the main method for checking if items can be inserted
     */
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    public void storagesense$checkMemoryBeforeInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // CRITICAL: Early exit for ANY player inventory operations (main inventory + hotbar)
        if (isPlayerInventorySlot()) {
            return;
        }
        
        // CRITICAL: Only apply when memory should be enforced
        if (!shouldEnforceMemory()) {
            return;
        }
        
        String containerId = getContainerId();
        if (containerId != null) {
            // Get container slot count - only filter container slots, not player inventory
            int containerSlots = getContainerSlotCount();
            if (containerSlots > 0 && index < containerSlots) {
                ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(containerId, index, containerSlots);
                
                // Only block if we have a specific template and the item doesn't match
                if (!template.isEmpty() && !itemMatchesTemplate(stack, template)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
        // If we get here, let vanilla logic handle it - NEVER block by default
    }
    
    /**
     * Intercept setStack to check memory configurations for direct slot setting
     * BE VERY CONSERVATIVE HERE - only block when absolutely certain
     */
    @Inject(method = "setStack", at = @At("HEAD"), cancellable = true)
    public void storagesense$checkMemoryBeforeSet(ItemStack stack, CallbackInfo ci) {
        // Early exit if this is a player inventory slot (main inventory + hotbar)
        if (isPlayerInventorySlot()) {
            return;
        }
        
        // Only block non-empty stacks when memory enforcement is active
        if (!stack.isEmpty() && shouldEnforceMemory()) {
            String containerId = getContainerId();
            if (containerId != null) {
                int containerSlots = getContainerSlotCount();
                // Be extra conservative - only block if we're certain this is a container slot
                // and we have a specific template that doesn't match
                if (containerSlots > 0 && index < containerSlots) {
                    ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(containerId, index, containerSlots);
                    
                    // Only cancel if we have a specific template and the item doesn't match
                    if (!template.isEmpty() && !itemMatchesTemplate(stack, template)) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }
        // NEVER cancel by default - let vanilla logic handle everything else
    }
    
    /**
     * Intercept setStackNoCallbacks for comprehensive coverage
     * BE VERY CONSERVATIVE HERE - only block when absolutely certain
     */
    @Inject(method = "setStackNoCallbacks", at = @At("HEAD"), cancellable = true)
    public void storagesense$checkMemoryBeforeSetNoCallbacks(ItemStack stack, CallbackInfo ci) {
        // Early exit if this is a player inventory slot (main inventory + hotbar)
        if (isPlayerInventorySlot()) {
            return;
        }
        
        // Only block non-empty stacks when memory enforcement is active
        if (!stack.isEmpty() && shouldEnforceMemory()) {
            String containerId = getContainerId();
            if (containerId != null) {
                int containerSlots = getContainerSlotCount();
                // Be extra conservative - only block if we're certain this is a container slot
                // and we have a specific template that doesn't match
                if (containerSlots > 0 && index < containerSlots) {
                    ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(containerId, index, containerSlots);
                    
                    // Only cancel if we have a specific template and the item doesn't match
                    if (!template.isEmpty() && !itemMatchesTemplate(stack, template)) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }
        // NEVER cancel by default - let vanilla logic handle everything else
    }
    
    /**
     * Check if we should enforce memory filtering
     */
    private boolean shouldEnforceMemory() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null && client.world.isClient;
    }
    
    /**
     * Get the container ID for the current screen handler
     */
    private String getContainerId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            // Try to get container ID from the screen handler
            // This approach mirrors what HandledScreenMixin does
            ContainerTracker.ContainerInfo info = ContainerTracker.getContainerInfo(handledScreen.getScreenHandler());
            if (info != null) {
                // Create a consistent container ID using the same method as HandledScreenMixin
                return EnhancedSBStyleMemoryManager.createContainerId(
                    handledScreen.getClass().getSimpleName(), 
                    handledScreen.getScreenHandler().syncId, 
                    info.pos()
                );
            }
        }
        return null;
    }
    
    /**
     * Get the number of container slots (excluding player inventory)
     */
    private int getContainerSlotCount() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            // Player inventory is always 36 slots, so container slots are the remainder
            return Math.max(0, handledScreen.getScreenHandler().slots.size() - 36);
        }
        return 0;
    }
    
    /**
     * Check if an item matches a memory template
     */
    private boolean itemMatchesTemplate(ItemStack item, ItemStack template) {
        if (template.isEmpty()) {
            return true; // Empty template accepts any item
        }
        if (item.isEmpty()) {
            return false; // Empty item can't match non-empty template
        }
        
        // Match by item type (ignoring stack size, NBT, etc. for basic matching)
        // This follows the SB pattern of matching by item type
        return item.getItem() == template.getItem();
    }
    
    /**
     * Check if this slot belongs to the player inventory (including hotbar)
     * Uses the most reliable method: checking if the slot's inventory is the player's inventory
     * This covers ALL player inventory slots: main inventory (27 slots) + hotbar (9 slots) = 36 total
     */
    private boolean isPlayerInventorySlot() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        
        // Primary method: Check if this slot's inventory is the player's inventory
        // This includes both main inventory and hotbar slots
        // PlayerInventory contains: main inventory (slots 9-35) + hotbar (slots 0-8)
        if (inventory == client.player.getInventory()) {
            return true;
        }
        
        // Additional safety check: If we're in a HandledScreen, check slot positioning
        // Player inventory typically occupies the last 36 slots in most containers
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            int totalSlots = handledScreen.getScreenHandler().slots.size();
            int containerSlots = Math.max(0, totalSlots - 36); // 36 = player inventory + hotbar
            
            // If this slot index is in the player inventory range, it's a player slot
            if (index >= containerSlots) {
                return true;
            }
        }
        
        return false;
    }
}
