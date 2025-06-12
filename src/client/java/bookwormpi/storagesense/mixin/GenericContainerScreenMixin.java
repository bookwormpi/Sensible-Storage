package bookwormpi.storagesense.mixin;

import bookwormpi.storagesense.client.container.ContainerTracker;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track when container screens are opened
 * This helps us associate screen handlers with block positions
 */
@Mixin(GenericContainerScreen.class)
public class GenericContainerScreenMixin {
    
    @Inject(method = "init", at = @At("TAIL"))
    private void storagesense$onContainerScreenInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            ScreenHandler handler = screen.getScreenHandler();
            
            if (handler instanceof GenericContainerScreenHandler) {
                // Try to find the container position
                // This is a simplified approach - in a full implementation,
                // we'd need more sophisticated position detection
                BlockPos containerPos = findNearestContainer(client);
                if (containerPos != null) {
                    ContainerTracker.registerContainer(handler, client.world, containerPos);
                }
            }
        }
    }
    
    @Inject(method = "close", at = @At("HEAD"))
    private void storagesense$onContainerScreenClose(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ContainerTracker.unregisterContainer(screen.getScreenHandler());
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
