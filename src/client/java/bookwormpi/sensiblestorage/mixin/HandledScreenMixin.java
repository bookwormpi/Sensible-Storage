package bookwormpi.sensiblestorage.mixin;

import bookwormpi.sensiblestorage.client.container.ContainerTracker;
import bookwormpi.sensiblestorage.client.gui.MemoryConfigScreen;
import bookwormpi.sensiblestorage.client.gui.widget.SBStyleMemoryButton;
import bookwormpi.sensiblestorage.client.memory.EnhancedSBStyleMemoryManager;
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
    
    private String sensiblestorage$containerId = null;
    private boolean sensiblestorage$isContainerScreen = false;
    private BlockPos sensiblestorage$containerPos = null;
    private long sensiblestorage$lastClickTime = 0;
    private long sensiblestorage$lastButtonClickTime = 0;
    private SBStyleMemoryButton sensiblestorage$memoryButton = null;
    
    @Inject(method = "init()V", at = @At("TAIL"))
    private void sensiblestorage$onContainerScreenInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            
            // Check if this is a container screen we want to add memory slots to
            sensiblestorage$isContainerScreen = isContainerScreen(screen);
            
            if (sensiblestorage$isContainerScreen) {
                // Initialize enhanced memory manager
                EnhancedSBStyleMemoryManager.init();
                
                // Find container position for persistent memory
                sensiblestorage$containerPos = findNearestContainer(client);
                
                // Create container ID (persistent if position found, session-based otherwise)
                sensiblestorage$containerId = EnhancedSBStyleMemoryManager.createContainerId(
                    screen.getClass().getSimpleName(), handler.syncId, sensiblestorage$containerPos);
                
                // Initialize memory templates
                int containerSlots = Math.max(0, handler.slots.size() - 36);
                EnhancedSBStyleMemoryManager.getMemoryTemplates(sensiblestorage$containerId, containerSlots);
                
                // Create SB-style memory button
                int memoryButtonX = x + backgroundWidth + 4;
                int memoryButtonY = y + 20;
                sensiblestorage$memoryButton = new SBStyleMemoryButton(
                    memoryButtonX, memoryButtonY, client.world, sensiblestorage$containerPos, screen);
            }
            
            // Original container tracking
            if (handler != null && hasInventory(handler)) {
                BlockPos containerPos = sensiblestorage$containerPos != null ? sensiblestorage$containerPos : findNearestContainer(client);
                if (containerPos != null) {
                    ContainerTracker.registerContainer(handler, client.world, containerPos);
                }
            }
        }
    }
    
    @Inject(method = "removed()V", at = @At("HEAD"))
    private void sensiblestorage$onContainerScreenClose(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ContainerTracker.unregisterContainer(screen.getScreenHandler());
        
        // Enhanced SB-style cleanup: remove or persist memory when screen closes
        if (sensiblestorage$containerId != null) {
            EnhancedSBStyleMemoryManager.removeContainer(sensiblestorage$containerId);
        }
    }
    
    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void sensiblestorage$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Check if M key was pressed (keyCode 77 = M)
        if (keyCode == GLFW.GLFW_KEY_M) {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            ContainerTracker.ContainerInfo containerInfo = ContainerTracker.getContainerInfo(screen.getScreenHandler());
            
            if (containerInfo != null) {
                
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
     * Render SB-style memory button and other controls
     */
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
    private void sensiblestorage$renderMemoryButtons(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!sensiblestorage$isContainerScreen || sensiblestorage$containerId == null) return;
        
        // Render SB-style memory button
        if (sensiblestorage$memoryButton != null) {
            sensiblestorage$memoryButton.render(context, mouseX, mouseY, delta);
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        
        // SB-style Clear button
        int clearButtonX = x + backgroundWidth + 4;
        int clearButtonY = y + 44;
        context.fill(clearButtonX, clearButtonY, clearButtonX + 60, clearButtonY + 20, 0xFF444444);
        context.fill(clearButtonX + 1, clearButtonY + 1, clearButtonX + 59, clearButtonY + 19, 0xFF666666);
        
        context.drawText(client.textRenderer, "Clear", clearButtonX + 4, clearButtonY + 6, 0xFFFFFF, false);
    }
    
    /**
     * Handle clicks on SB-style memory buttons
     */
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void sensiblestorage$handleMemoryButtonClicks(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!sensiblestorage$isContainerScreen || sensiblestorage$containerId == null) return;
        
        // Debounce button clicks to prevent rapid toggling when holding down mouse
        long currentTime = System.currentTimeMillis();
        if (currentTime - sensiblestorage$lastButtonClickTime < 200) {
            return; // Ignore rapid clicks
        }
        
        int memoryButtonX = x + backgroundWidth + 4;
        int memoryButtonY = y + 20;
        int clearButtonX = x + backgroundWidth + 4;
        int clearButtonY = y + 44;
        
        // Check SB-style Memory button click
        if (sensiblestorage$memoryButton != null && 
            mouseX >= sensiblestorage$memoryButton.getX() && mouseX <= sensiblestorage$memoryButton.getX() + sensiblestorage$memoryButton.getWidth() && 
            mouseY >= sensiblestorage$memoryButton.getY() && mouseY <= sensiblestorage$memoryButton.getY() + sensiblestorage$memoryButton.getHeight()) {
            sensiblestorage$lastButtonClickTime = currentTime;
            
            // Let the button handle its own click
            sensiblestorage$memoryButton.mouseClicked(mouseX, mouseY, button);
            
            cir.setReturnValue(true);
            return;
        }
        
        // Check Clear button click
        if (mouseX >= clearButtonX && mouseX <= clearButtonX + 60 && 
            mouseY >= clearButtonY && mouseY <= clearButtonY + 20) {
            sensiblestorage$lastButtonClickTime = currentTime;
            
            // SB-style memory clear
            EnhancedSBStyleMemoryManager.clearMemory(sensiblestorage$containerId);
            cir.setReturnValue(true);
            return;
        }
        

    }
    
    /**
     * Render ghost items in empty slots with memory templates
     * Based on SophisticatedCore's rendering approach
     */
    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;)V", at = @At("TAIL"))
    private void sensiblestorage$renderGhostItems(DrawContext context, Slot slot, CallbackInfo ci) {
        if (!sensiblestorage$isContainerScreen || sensiblestorage$containerId == null) return;
        
        // Only render for container slots (not player inventory)
        int slotIndex = handler.slots.indexOf(slot);
        int containerSlots = Math.max(0, handler.slots.size() - 36);
        if (slotIndex < 0 || slotIndex >= containerSlots) return;
        
        boolean memorizeMode = EnhancedSBStyleMemoryManager.isMemorizeMode(sensiblestorage$containerId);
        
        // Render ghost items like SophisticatedCore
        if (slot.getStack().isEmpty()) {
            ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(sensiblestorage$containerId, slotIndex, containerSlots);
            if (!template.isEmpty()) {
                // Render the template item
                context.drawItem(template, slot.x, slot.y);
                
                // Add a semi-transparent overlay to create the ghost effect
                sensiblestorage$drawStackOverlay(context, slot.x, slot.y);
            }
        }
        
        // In memorize mode, highlight empty slots that can receive templates
        if (memorizeMode && slot.getStack().isEmpty()) {
            ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(sensiblestorage$containerId, slotIndex, containerSlots);
            if (template.isEmpty()) {
                // Highlight empty slots that can receive memory templates
                context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x4000FF00); // Green highlight
            }
        }
    }
    
    /**
     * Draw overlay on ghost items to make them appear translucent
     * Based on SophisticatedCore's approach
     */
    private void sensiblestorage$drawStackOverlay(DrawContext context, int x, int y) {
        // Create a semi-transparent white overlay to give the ghost effect
        // This matches how SophisticatedCore renders ghost items
        context.fill(x, y, x + 16, y + 16, 0x80FFFFFF); // 50% transparent white
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
     * Handle slot clicks for both memorize mode and filtering
     * Based on SophisticatedCore's MemorySettingsTab approach
     */
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void sensiblestorage$handleSlotClick(net.minecraft.screen.slot.Slot slot, int slotId, int button, net.minecraft.screen.slot.SlotActionType actionType, CallbackInfo ci) {
        if (!sensiblestorage$isContainerScreen || sensiblestorage$containerId == null) return;
        
        // Only handle container slots, not player inventory
        int slotIndex = handler.slots.indexOf(slot);
        int containerSlots = Math.max(0, handler.slots.size() - 36);
        if (slotIndex < 0 || slotIndex >= containerSlots) return;
        
        boolean isMemorizeMode = EnhancedSBStyleMemoryManager.isMemorizeMode(sensiblestorage$containerId);
        
        if (isMemorizeMode) {
            // Handle memorize mode - setting memory templates
            sensiblestorage$handleMemorizeMode(slot, slotIndex, button, containerSlots, ci);
        } else {
            // Handle filtering - prevent placing items that don't match templates
            sensiblestorage$handleSlotFiltering(slot, slotIndex, button, actionType, containerSlots, ci);
        }
    }
    
    /**
     * Handle memorize mode slot clicks
     */
    private void sensiblestorage$handleMemorizeMode(net.minecraft.screen.slot.Slot slot, int slotIndex, int button, int containerSlots, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Debounce rapid clicks (prevent setting same template multiple times quickly)
        long currentTime = System.currentTimeMillis();
        if (currentTime - sensiblestorage$lastClickTime < 100) {
            ci.cancel();
            return;
        }
        sensiblestorage$lastClickTime = currentTime;

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
        
        // Set the memory template
        if (button == 0) { // Left click sets template
            EnhancedSBStyleMemoryManager.setTemplate(sensiblestorage$containerId, slotIndex, templateItem, containerSlots);
        } else if (button == 1) { // Right click clears template
            EnhancedSBStyleMemoryManager.setTemplate(sensiblestorage$containerId, slotIndex, ItemStack.EMPTY, containerSlots);
        }

        // Cancel the normal slot action to prevent item movement
        ci.cancel();
    }
    
    /**
     * Handle slot filtering - prevent placing items that don't match memory templates
     */
    private void sensiblestorage$handleSlotFiltering(net.minecraft.screen.slot.Slot slot, int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, int containerSlots, CallbackInfo ci) {
        // Get the memory template for this slot
        ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(sensiblestorage$containerId, slotIndex, containerSlots);
        if (template.isEmpty()) return; // No template set, allow any item
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // Get the item being placed
        ItemStack itemToPlace = ItemStack.EMPTY;
        
        // Different actions place items from different sources
        switch (actionType) {
            case PICKUP:
                // Placing cursor item into slot
                itemToPlace = handler.getCursorStack();
                if (itemToPlace.isEmpty()) {
                    itemToPlace = client.player.playerScreenHandler.getCursorStack();
                }
                break;
                
            case QUICK_MOVE:
                // Shift-clicking - need to handle this properly
                sensiblestorage$handleShiftClickFiltering(slot, slotIndex, containerSlots, ci);
                return;
                
            case SWAP:
                // Hotbar swap (1-9 keys)
                if (button >= 0 && button < 9) {
                    itemToPlace = client.player.getInventory().getStack(button);
                }
                break;
                
            case CLONE:
                // Creative mode middle-click - allow this
                return;
                
            case THROW:
                // Dropping items - not placing, so allow
                return;
                
            default:
                // For other actions, try to get cursor stack
                itemToPlace = handler.getCursorStack();
                if (itemToPlace.isEmpty()) {
                    itemToPlace = client.player.playerScreenHandler.getCursorStack();
                }
                break;
        }
        
        // If we're trying to place an item and it doesn't match the template, block it
        if (!itemToPlace.isEmpty() && !EnhancedSBStyleMemoryManager.itemMatchesTemplate(template, itemToPlace)) {
            ci.cancel(); // Block the slot interaction
            return;
        }
        
        // If we're taking an item from a slot with a template, that's always allowed
        // (template filtering only applies to putting items IN)
    }
    
    /**
     * Handle shift-click filtering specifically
     * This is more complex because we need to determine which slots the item will go to
     */
    private void sensiblestorage$handleShiftClickFiltering(Slot slot, int slotIndex, int containerSlots, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        ItemStack stackToMove = slot.getStack();
        if (stackToMove.isEmpty()) return;
        
        // Determine if we're moving FROM container TO inventory or FROM inventory TO container
        boolean isContainerSlot = slotIndex >= 0 && slotIndex < containerSlots;
        
        if (isContainerSlot) {
            // Moving FROM container TO inventory - this is always allowed (taking items out)
            return;
        } else {
            // Moving FROM inventory TO container - need to check where it will go
            // We need to simulate where the item would go and check if those slots allow it
            
            // Find potential destination slots in the container
            for (int destSlotIndex = 0; destSlotIndex < containerSlots; destSlotIndex++) {
                Slot destSlot = handler.getSlot(destSlotIndex);
                ItemStack destStack = destSlot.getStack();
                
                // Check if this slot could accept some of the items
                if (destStack.isEmpty() || ItemStack.areItemsAndComponentsEqual(destStack, stackToMove)) {
                    // This slot is a potential destination
                    ItemStack template = EnhancedSBStyleMemoryManager.getTemplate(sensiblestorage$containerId, destSlotIndex, containerSlots);
                    
                    if (!template.isEmpty() && !EnhancedSBStyleMemoryManager.itemMatchesTemplate(template, stackToMove)) {
                        // Template exists and item doesn't match - block the shift-click
                        ci.cancel();
                        return;
                    }
                }
            }
            
            // If we get here, either no templates conflict or no valid destination slots found
            // Allow the operation to proceed
        }
    }
}