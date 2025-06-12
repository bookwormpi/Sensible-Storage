package bookwormpi.storagesense.mixin;

import bookwormpi.storagesense.client.container.ContainerTracker;
import bookwormpi.storagesense.memory.ClientMemoryManager;
import bookwormpi.storagesense.memory.MemorySlot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
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
 * Inspired by Sophisticated Backpacks' approach but implemented independently
 */
@Mixin(Slot.class)
public class SlotMixin {
    
    @Shadow @Final
    public Inventory inventory;
    
    @Shadow
    private int index;
    
    /**
     * Intercept canInsert to check memory configurations
     */
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    public void storagesense$checkMemoryBeforeInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.world.isClient) {
            // Only run on client side (since this is a client-only mod)
            if (shouldEnforceMemory(client.world)) {
                BlockPos containerPos = getContainerPosition();
                if (containerPos != null) {
                    MemorySlot memoryConfig = ClientMemoryManager.getInstance()
                        .getSlotMemory(client.world, containerPos, index);
                    
                    if (memoryConfig.isConfigured() && !memoryConfig.canAcceptItem(stack)) {
                        // Memory is configured for this slot and this item is not allowed
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
        // If we get here, let vanilla logic handle it
    }
    
    /**
     * Intercept setStack to check memory configurations for direct slot setting
     */
    @Inject(method = "setStack", at = @At("HEAD"), cancellable = true)
    public void storagesense$checkMemoryBeforeSet(ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && shouldEnforceMemory(client.world)) {
                BlockPos containerPos = getContainerPosition();
                if (containerPos != null) {
                    MemorySlot memoryConfig = ClientMemoryManager.getInstance()
                        .getSlotMemory(client.world, containerPos, index);
                    
                    if (memoryConfig.isConfigured() && !memoryConfig.canAcceptItem(stack)) {
                        // Prevent the stack from being set
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Intercept setStackNoCallbacks for comprehensive coverage
     */
    @Inject(method = "setStackNoCallbacks", at = @At("HEAD"), cancellable = true)
    public void storagesense$checkMemoryBeforeSetNoCallbacks(ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && shouldEnforceMemory(client.world)) {
                BlockPos containerPos = getContainerPosition();
                if (containerPos != null) {
                    MemorySlot memoryConfig = ClientMemoryManager.getInstance()
                        .getSlotMemory(client.world, containerPos, index);
                    
                    if (memoryConfig.isConfigured() && !memoryConfig.canAcceptItem(stack)) {
                        // Prevent the stack from being set
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Check if we should enforce memory for this world/container type
     */
    private boolean shouldEnforceMemory(World world) {
        // For now, enforce for all containers
        // Later we can add configuration options
        return true;
    }
    
    /**
     * Try to determine the container's position in the world
     * Uses our container tracker system
     */
    private BlockPos getContainerPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> handledScreen) {
            ContainerTracker.ContainerInfo info = ContainerTracker.getContainerInfo(handledScreen.getScreenHandler());
            if (info != null) {
                return info.pos();
            }
        }
        
        return null; // Could not determine container position
    }
}
