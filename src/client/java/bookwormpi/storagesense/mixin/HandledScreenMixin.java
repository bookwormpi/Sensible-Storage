package bookwormpi.storagesense.mixin;

import bookwormpi.storagesense.StorageSense;
import bookwormpi.storagesense.client.container.ContainerTracker;
import bookwormpi.storagesense.client.gui.MemoryConfigScreen;
import bookwormpi.storagesense.client.memory.EnhancedSBStyleMemoryManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to track when container screens are opened and add SB-style memory slots
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    
    @Shadow @Final protected ScreenHandler handler;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    
    private String storagesense$containerId = null;
    private boolean storagesense$isContainerScreen = false;
    private BlockPos storagesense$containerPos = null;
    private long storagesense$lastClickTime = 0;
    
    @Inject(method = "init()V", at = @At("TAIL"))
    private void storagesense$onContainerScreenInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            StorageSense.LOGGER.info("HandledScreen init: {}, slots: {}", 
                screen.getClass().getSimpleName(), handler.slots.size());
            
            // Check if this is a container screen we want to add memory slots to
            storagesense$isContainerScreen = isContainerScreen(screen);
            
            if (storagesense$isContainerScreen) {
                // Initialize enhanced memory manager
                EnhancedSBStyleMemoryManager.init();
                
                // Find container position for persistent memory
                storagesense$containerPos = findNearestContainer(client);
                
                // Create container ID (persistent if position found, session-based otherwise)
                storagesense$containerId = EnhancedSBStyleMemoryManager.createContainerId(
                    screen.getClass().getSimpleName(), handler.syncId, storagesense$containerPos);
                
                // Initialize memory templates
                int containerSlots = Math.max(0, handler.slots.size() - 36);
                EnhancedSBStyleMemoryManager.getMemoryTemplates(storagesense$containerId, containerSlots);
                
                StorageSense.LOGGER.info("Initialized enhanced SB-style memory for container: {} (pos: {})", 
                    storagesense$containerId, storagesense$containerPos);
            }
            
            // Original container tracking
            if (handler != null && hasInventory(handler)) {
                BlockPos containerPos = storagesense$containerPos != null ? storagesense$containerPos : findNearestContainer(client);
                if (containerPos != null) {
                    ContainerTracker.registerContainer(handler, client.world, containerPos);
                    StorageSense.LOGGER.info("Registered container at position: {}", containerPos);
                }
            }
        }
    }
    
    @Inject(method = "removed()V", at = @At("HEAD"))
    private void storagesense$onContainerScreenClose(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ContainerTracker.unregisterContainer(screen.getScreenHandler());
        
        // Enhanced SB-style cleanup: remove or persist memory when screen closes
        if (storagesense$containerId != null) {
            EnhancedSBStyleMemoryManager.removeContainer(storagesense$containerId);
            StorageSense.LOGGER.info("Closed container: {}", storagesense$containerId);
        }
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
        return blockEntity instanceof net.minecraft.block.entity.ChestBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.BarrelBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.ShulkerBoxBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.HopperBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.DispenserBlockEntity ||
               blockEntity instanceof net.minecraft.block.entity.DropperBlockEntity;
    }
    
    /**
     * Add SB-style memory and clear buttons
     */
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
    private void storagesense$renderMemoryButtons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!storagesense$isContainerScreen || storagesense$containerId == null) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        boolean memorizeMode = EnhancedSBStyleMemoryManager.isMemorizeMode(storagesense$containerId);
        
        // SB-style Memory button - changes color when active
        int memoryButtonX = x + backgroundWidth + 4;
        int memoryButtonY = y + 20;
        int memoryBgColor = memorizeMode ? 0xFF4444FF : 0xFF444444; // Blue when active
        int memoryFgColor = memorizeMode ? 0xFF6666FF : 0xFF666666;
        
        context.fill(memoryButtonX, memoryButtonY, memoryButtonX + 60, memoryButtonY + 20, memoryBgColor);
        context.fill(memoryButtonX + 1, memoryButtonY + 1, memoryButtonX + 59, memoryButtonY + 19, memoryFgColor);
        
        String memoryText = memorizeMode ? "Memory*" : "Memory";
        context.drawText(client.textRenderer, memoryText, memoryButtonX + 4, memoryButtonY + 6, 0xFFFFFF, false);
        
        // SB-style Clear button  
        int clearButtonX = x + backgroundWidth + 4;
        int clearButtonY = y + 44;
        context.fill(clearButtonX, clearButtonY, clearButtonX + 60, clearButtonY + 20, 0xFF444444);
        context.fill(clearButtonX + 1, clearButtonY + 1, clearButtonX + 59, clearButtonY + 19, 0xFF666666);
        
        context.drawText(client.textRenderer, "Clear", clearButtonX + 4, clearButtonY + 6, 0xFFFFFF, false);
        
        // Persistent mode toggle button
        int persistButtonX = x + backgroundWidth + 4;
        int persistButtonY = y + 68;
        boolean isPersistent = EnhancedSBStyleMemoryManager.isPersistentMode();
        int persistBgColor = isPersistent ? 0xFF444444 : 0xFF222222;
        int persistFgColor = isPersistent ? 0xFF666666 : 0xFF444444;
        
        context.fill(persistButtonX, persistButtonY, persistButtonX + 60, persistButtonY + 20, persistBgColor);
        context.fill(persistButtonX + 1, persistButtonY + 1, persistButtonX + 59, persistButtonY + 19, persistFgColor);
        
        String persistText = isPersistent ? "Persist*" : "Session";
        context.drawText(client.textRenderer, persistText, persistButtonX + 4, persistButtonY + 6, 
            isPersistent ? 0xFFFFFF : 0xFFAAAAAA, false);
        
        // Add small indicator text showing container ID type
        if (isPersistent && storagesense$containerPos != null) {
            String posText = String.format("@%d,%d,%d", 
                storagesense$containerPos.getX(), 
                storagesense$containerPos.getY(), 
                storagesense$containerPos.getZ());
            context.drawText(client.textRenderer, posText, persistButtonX + 4, persistButtonY + 22, 0xFF888888, false);
        }
    }
    
    /**
     * Handle clicks on SB-style memory buttons
     */
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void storagesense$handleMemoryButtonClicks(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!storagesense$isContainerScreen || storagesense$containerId == null) return;
        
        int memoryButtonX = x + backgroundWidth + 4;
        int memoryButtonY = y + 20;
        int clearButtonX = x + backgroundWidth + 4;
        int clearButtonY = y + 44;
        int persistButtonX = x + backgroundWidth + 4;
        int persistButtonY = y + 68;
        
        // Check Memory button click
        if (mouseX >= memoryButtonX && mouseX <= memoryButtonX + 60 && 
            mouseY >= memoryButtonY && mouseY <= memoryButtonY + 20) {
            // SB-style memory mode toggle
            boolean newMode = EnhancedSBStyleMemoryManager.toggleMemorizeMode(storagesense$containerId);
            
            if (newMode) {
                StorageSense.LOGGER.info("Entered memorize mode for container: {}", storagesense$containerId);
            } else {
                StorageSense.LOGGER.info("Exited memorize mode for container: {}", storagesense$containerId);
            }
            cir.setReturnValue(true);
            return;
        }
        
        // Check Clear button click
        if (mouseX >= clearButtonX && mouseX <= clearButtonX + 60 && 
            mouseY >= clearButtonY && mouseY <= clearButtonY + 20) {
            // SB-style memory clear
            EnhancedSBStyleMemoryManager.clearMemory(storagesense$containerId);
            StorageSense.LOGGER.info("Cleared memory for container: {}", storagesense$containerId);
            cir.setReturnValue(true);
            return;
        }
        
        // Check Persistent mode toggle button click
        if (mouseX >= persistButtonX && mouseX <= persistButtonX + 60 && 
            mouseY >= persistButtonY && mouseY <= persistButtonY + 20) {
            // Toggle persistent mode
            boolean newPersistentMode = !EnhancedSBStyleMemoryManager.isPersistentMode();
            EnhancedSBStyleMemoryManager.setPersistentStorage(newPersistentMode);
            
            StorageSense.LOGGER.info("Toggled persistent memory mode to: {}", newPersistentMode ? "ON" : "OFF");
            
            // Recreate container ID with new mode
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
                String newContainerId = EnhancedSBStyleMemoryManager.createContainerId(
                    screen.getClass().getSimpleName(), handler.syncId, storagesense$containerPos);
                
                // Migrate memory if container ID changed
                if (!newContainerId.equals(storagesense$containerId)) {
                    StorageSense.LOGGER.info("Container ID changed from {} to {} due to mode toggle", 
                        storagesense$containerId, newContainerId);
                    
                    // Migrate existing memory templates to new container ID
                    EnhancedSBStyleMemoryManager.migrateMemory(storagesense$containerId, newContainerId);
                    
                    storagesense$containerId = newContainerId;
                    
                    // Reinitialize memory templates (they should now have migrated data)
                    int containerSlots = Math.max(0, handler.slots.size() - 36);
                    EnhancedSBStyleMemoryManager.getMemoryTemplates(storagesense$containerId, containerSlots);
                }
            }
            
            cir.setReturnValue(true);
        }
    }
    
    /**
     * Render ghost items in empty slots with memory templates (SB-style)
     */
    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V", at = @At("TAIL"))
    private void storagesense$renderGhostItems(DrawContext context, Slot slot, CallbackInfo ci) {
        if (!storagesense$isContainerScreen || storagesense$containerId == null) return;
        
        // Only render for container slots (not player inventory)
        int slotIndex = handler.slots.indexOf(slot);
        int containerSlots = Math.max(0, handler.slots.size() - 36);
        if (slotIndex < 0 || slotIndex >= containerSlots) return;
        
        boolean memorizeMode = EnhancedSBStyleMemoryManager.isMemorizeMode(storagesense$containerId);
        
        // SB-style ghost item rendering
        if (slot.getStack().isEmpty()) {
            ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(storagesense$containerId, slotIndex, containerSlots);
            if (!template.isEmpty()) {
                // Debug logging to see what's being rendered
                StorageSense.LOGGER.info("Rendering ghost item for slot {}: {}", slotIndex, template.getItem().toString());
                
                // Render ghost item with REI-style transparency
                context.getMatrices().push();
                
                // Reduce the Z level slightly to render behind normal items
                context.getMatrices().translate(0, 0, -50);
                
                // Render the item normally first
                context.drawItem(template, slot.x, slot.y);
                
                // Move back to normal Z level for overlay
                context.getMatrices().translate(0, 0, 50);
                
                // Apply a white semi-transparent overlay to create ghost effect
                // Different intensity for memorize mode vs normal mode
                float overlayAlpha = memorizeMode ? 0.4f : 0.6f;
                int overlayColor = (int)(overlayAlpha * 255) << 24 | 0xFFFFFF; // White overlay with alpha
                context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, overlayColor);
                
                context.getMatrices().pop();
            } else {
                // Debug: log when no template is found for empty slots
                StorageSense.LOGGER.info("No template found for empty slot {}", slotIndex);
            }
        }
        
        // In memorize mode, highlight empty slots that can receive templates
        if (memorizeMode && slot.getStack().isEmpty()) {
            ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(storagesense$containerId, slotIndex, containerSlots);
            if (template.isEmpty()) {
                // Highlight empty slots that can receive memory templates
                context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x4000FF00); // Green highlight
            }
        }
    }
    
    /**
     * Check if this is a container screen we want to add memory to
     */
    private boolean isContainerScreen(HandledScreen<?> screen) {
        String className = screen.getClass().getSimpleName();
        return className.contains("Chest") || 
               className.contains("Container") || 
               className.contains("Barrel") ||
               className.contains("Shulker");
    }
    
    /**
     * Handle slot clicks to set memory in memorize mode
     * Based on SophisticatedCore's MemorySettingsTab approach
     */
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void storagesense$handleMemorizeSlotClick(net.minecraft.screen.slot.Slot slot, int slotId, int button, net.minecraft.screen.slot.SlotActionType actionType, CallbackInfo ci) {
        if (!storagesense$isContainerScreen || storagesense$containerId == null) return;
        if (!EnhancedSBStyleMemoryManager.isMemorizeMode(storagesense$containerId)) return;
        
        // Only handle container slots, not player inventory
        int slotIndex = handler.slots.indexOf(slot);
        int containerSlots = Math.max(0, handler.slots.size() - 36);
        if (slotIndex < 0 || slotIndex >= containerSlots) return;
        
        // In memorize mode, clicking on a slot sets its memory template
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Debounce rapid clicks (prevent setting same template multiple times quickly)
            long currentTime = System.currentTimeMillis();
            if (currentTime - storagesense$lastClickTime < 100) {
                ci.cancel();
                return;
            }
            storagesense$lastClickTime = currentTime;

            // Use SophisticatedCore's approach: try different sources for the item
            ItemStack templateItem = ItemStack.EMPTY;
            
            // First, try what's in the slot itself (if any)
            if (!slot.getStack().isEmpty()) {
                templateItem = slot.getStack().copy();
                templateItem.setCount(1); // Memory template should have count 1
            } else {
                // If slot is empty, try to get cursor stack
                // Use multiple methods for better compatibility
                ItemStack playerCursor = client.player.playerScreenHandler.getCursorStack();
                ItemStack handlerCursor = handler.getCursorStack();
                
                // Prefer the handler cursor as it's more specific to this container
                if (!handlerCursor.isEmpty()) {
                    templateItem = handlerCursor.copy();
                    templateItem.setCount(1);
                } else if (!playerCursor.isEmpty()) {
                    templateItem = playerCursor.copy();
                    templateItem.setCount(1);
                } else {
                    // If no cursor item, try main hand item
                    ItemStack mainHandItem = client.player.getMainHandStack();
                    if (!mainHandItem.isEmpty()) {
                        templateItem = mainHandItem.copy();
                        templateItem.setCount(1);
                    }
                }
            }
            
            // Debug logging to help identify issues
            StorageSense.LOGGER.info("Memorize Mode - Setting template for slot {} to: {}", 
                slotIndex, templateItem.isEmpty() ? "empty" : templateItem.getItem().toString());

            // Set the memory template
            if (button == 0) { // Left click sets template
                EnhancedSBStyleMemoryManager.setTemplate(storagesense$containerId, slotIndex, templateItem, containerSlots);
                StorageSense.LOGGER.info("Set memory template for slot {} to: {}", 
                    slotIndex, templateItem.isEmpty() ? "empty" : templateItem.getItem().toString());
            } else if (button == 1) { // Right click clears template
                EnhancedSBStyleMemoryManager.setTemplate(storagesense$containerId, slotIndex, ItemStack.EMPTY, containerSlots);
                StorageSense.LOGGER.info("Cleared memory template for slot {}", slotIndex);
            }

            // Cancel the normal slot action to prevent item movement
            ci.cancel();
        }
    }
}