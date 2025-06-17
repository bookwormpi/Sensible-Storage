package bookwormpi.sensiblestorage.mixin;

import bookwormpi.sensiblestorage.client.memory.ChestMemoryManager;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SB-style slot interaction filtering for memory slots
 */
@Mixin(Slot.class)
public class MemorySlotMixin {
    
    /**
     * Prevent inserting items that don't match memory template (SB approach)
     */
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void sensiblestorage$checkMemoryTemplate(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Slot slot = (Slot) (Object) this;
        
        // Only check container slots, not player inventory
        if (slot.inventory == null) return;
        
        // Get container ID (simplified approach - use inventory hash)
        int containerId = slot.inventory.hashCode();
        
        // Check if this container has memory initialized
        if (!ChestMemoryManager.isInitialized(containerId)) return;
        
        // SB-style: Check if item matches memory template
        if (!ChestMemoryManager.matchesTemplate(containerId, slot.id, stack)) {
            // Block insertion if item doesn't match template
            cir.setReturnValue(false);
        }
    }
}
