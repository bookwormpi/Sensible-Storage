package bookwormpi.storagesense.client.keybind;

import bookwormpi.storagesense.StorageSense;
import bookwormpi.storagesense.client.container.ContainerTracker;
import bookwormpi.storagesense.client.gui.MemoryConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Handles keybinds for Storage Sense functionality
 * Inspired by Sophisticated Backpacks' keybind system
 * 
 * Credit: Based on patterns from Salandora/SophisticatedBackpacks (GPL-3.0)
 * https://github.com/Salandora/SophisticatedBackpacks/blob/main/src/main/java/net/p3pp3rf1y/sophisticatedbackpacks/client/KeybindHandler.java
 */
public class KeyBindings {
    
    public static final KeyBinding CONFIGURE_MEMORY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.storagesense.configure_memory",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_M,
        "category.storagesense.keybinds"
    ));
    
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CONFIGURE_MEMORY.wasPressed()) {
                handleConfigureMemoryKey(client);
            }
        });
        
        StorageSense.LOGGER.info("Storage Sense keybinds initialized");
    }
    
    private static void handleConfigureMemoryKey(MinecraftClient client) {
        if (client.currentScreen instanceof HandledScreen<?> handledScreen) {
            ContainerTracker.ContainerInfo containerInfo = ContainerTracker.getContainerInfo(handledScreen.getScreenHandler());
            
            if (containerInfo != null) {
                // Get the slot under the mouse cursor
                // Adapted from Sophisticated Backpacks' slot detection pattern
                Slot hoveredSlot = getSlotUnderMouse(handledScreen);
                
                if (hoveredSlot != null && isValidContainerSlot(hoveredSlot)) {
                    int slotIndex = hoveredSlot.id;
                    
                    MemoryConfigScreen memoryScreen = new MemoryConfigScreen(
                        containerInfo.world(),
                        containerInfo.pos(),
                        slotIndex,
                        client.currentScreen
                    );
                    
                    client.setScreen(memoryScreen);
                } else {
                    // Show a message that no valid slot is selected
                    if (client.player != null) {
                        client.player.sendMessage(Text.translatable("storagesense.message.no_slot_selected"), false);
                    }
                }
            }
        }
    }
    
    /**
     * Gets the slot under the mouse cursor.
     * 
     * Based on Sophisticated Backpacks' slot detection logic (GPL-3.0).
     * Uses a similar approach to their findSlot method.
     */
    private static Slot getSlotUnderMouse(HandledScreen<?> screen) {
        try {
            // Get current mouse position
            MinecraftClient client = MinecraftClient.getInstance();
            double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
            double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
            
            return findSlotAt(screen, mouseX, mouseY);
        } catch (Exception e) {
            StorageSense.LOGGER.warn("Failed to get slot under mouse: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Finds the slot at the given mouse coordinates.
     * 
     * Implementation inspired by Sophisticated Backpacks' slot detection.
     */
    private static Slot findSlotAt(HandledScreen<?> screen, double mouseX, double mouseY) {
        // Use reflection to access private fields, or use the x/y accessors if available
        int guiLeft, guiTop;
        try {
            // Try to get the GUI position using reflection for backgroundWidth and backgroundHeight
            java.lang.reflect.Field backgroundWidthField = HandledScreen.class.getDeclaredField("backgroundWidth");
            java.lang.reflect.Field backgroundHeightField = HandledScreen.class.getDeclaredField("backgroundHeight");
            backgroundWidthField.setAccessible(true);
            backgroundHeightField.setAccessible(true);
            
            int backgroundWidth = backgroundWidthField.getInt(screen);
            int backgroundHeight = backgroundHeightField.getInt(screen);
            
            guiLeft = (screen.width - backgroundWidth) / 2;
            guiTop = (screen.height - backgroundHeight) / 2;
        } catch (Exception e) {
            // Fallback to standard GUI dimensions if reflection fails
            guiLeft = (screen.width - 176) / 2;  // Standard inventory width
            guiTop = (screen.height - 166) / 2;  // Standard inventory height
        }
        
        // Convert to relative coordinates within the GUI
        int relativeX = (int) mouseX - guiLeft;
        int relativeY = (int) mouseY - guiTop;
        
        // Check each slot to see if the mouse is over it
        for (Slot slot : screen.getScreenHandler().slots) {
            if (relativeX >= slot.x && relativeX < slot.x + 16 &&
                relativeY >= slot.y && relativeY < slot.y + 16) {
                return slot;
            }
        }
        
        return null;
    }
    
    /**
     * Check if the hovered slot is a valid container slot (not player inventory)
     * Inspired by Sophisticated Backpacks' container slot validation
     */
    private static boolean isValidContainerSlot(Slot slot) {
        // Check if this is a container slot and not a player inventory slot
        // This prevents configuring player inventory slots
        return !(slot.inventory instanceof PlayerInventory);
    }
}
