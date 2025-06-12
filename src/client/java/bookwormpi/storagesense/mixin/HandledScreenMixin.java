package bookwormpi.storagesense.mixin;

import bookwormpi.storagesense.StorageSense;
import bookwormpi.storagesense.client.container.ContainerTracker;
import bookwormpi.storagesense.client.gui.MemoryConfigScreen;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to track when container screens are opened
 * This helps us associate screen handlers with block positions
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    
    @Inject(method = "init()V", at = @At("TAIL"))
    private void storagesense$onContainerScreenInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            ScreenHandler handler = screen.getScreenHandler();
            
            StorageSense.LOGGER.info("HandledScreen init: {}, slots: {}", 
                screen.getClass().getSimpleName(), handler.slots.size()); // Debug log
            
            // Only track containers that have an inventory
            if (handler != null && hasInventory(handler)) {
                // Try to find the container position
                BlockPos containerPos = findNearestContainer(client);
                if (containerPos != null) {
                    ContainerTracker.registerContainer(handler, client.world, containerPos);
                    StorageSense.LOGGER.info("Registered container at position: {}", containerPos); // Debug log
                } else {
                    StorageSense.LOGGER.info("Could not find container position"); // Debug log
                }
            } else {
                StorageSense.LOGGER.info("Screen handler does not have inventory or has too few slots"); // Debug log
            }
        }
    }
    
    @Inject(method = "removed()V", at = @At("HEAD"))
    private void storagesense$onContainerScreenClose(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ContainerTracker.unregisterContainer(screen.getScreenHandler());
    }
    
    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void storagesense$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Check if M key was pressed (keyCode 77 = M)
        if (keyCode == GLFW.GLFW_KEY_M) {
            StorageSense.LOGGER.info("M key pressed in container screen!");
            
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            ContainerTracker.ContainerInfo containerInfo = ContainerTracker.getContainerInfo(screen.getScreenHandler());
            
            if (containerInfo != null) {
                StorageSense.LOGGER.info("Opening memory config for container at: {}", containerInfo.pos());
                
                // Open memory config for slot 0 as a test
                MemoryConfigScreen memoryScreen = new MemoryConfigScreen(
                    containerInfo.world(),
                    containerInfo.pos(),
                    0,
                    MinecraftClient.getInstance().currentScreen
                );
                
                MinecraftClient.getInstance().setScreen(memoryScreen);
                
                // Mark the key as handled to prevent further processing
                cir.setReturnValue(true);
            }
        }
    }
    
    /**
     * Check if a screen handler has an inventory that we should track
     */
    private boolean hasInventory(ScreenHandler handler) {
        // Most container screen handlers we care about will have slots
        // We can be more specific if needed
        return handler.slots.size() > 36; // Player inventory is 36 slots, so containers have more
    }
    
    /**
     * Find the nearest container block to the player
     * This is a simple heuristic - a more sophisticated approach would be needed
     * for production use
     */
    private BlockPos findNearestContainer(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return null;
        }
        
        BlockPos playerPos = client.player.getBlockPos();
        
        // Search in a small radius around the player
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    BlockEntity blockEntity = client.world.getBlockEntity(checkPos);
                    
                    // Check if this is a container block entity
                    if (blockEntity != null && isContainerBlockEntity(blockEntity)) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if a block entity is a container that we should track
     */
    private boolean isContainerBlockEntity(BlockEntity blockEntity) {
        // Check for common container types
        return blockEntity instanceof net.minecraft.block.entity.ChestBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.BarrelBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.ShulkerBoxBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.HopperBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.DispenserBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.DropperBlockEntity;
        
        // Note: We could extend this to support mod containers by checking for
        // inventory interfaces or using duck typing
    }
}