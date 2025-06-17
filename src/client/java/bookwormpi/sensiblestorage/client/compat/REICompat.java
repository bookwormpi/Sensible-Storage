package bookwormpi.sensiblestorage.client.compat;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.util.Collection;
import java.util.Collections;

/**
 * REI compatibility plugin for Storage Sense
 * Registers exclusion zones to prevent REI panels from overlapping with our memory buttons
 */
public class REICompat implements REIClientPlugin {
    
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        // Register exclusion zones for container screens with memory buttons
        zones.register(HandledScreen.class, this::getExclusionZones);
    }
    
    /**
     * Get exclusion zones for container screens
     * This prevents REI panels from overlapping with our memory buttons
     */
    private Collection<Rectangle> getExclusionZones(HandledScreen<?> screen) {
        // Check if this is a container screen we add memory buttons to
        if (!isContainerScreen(screen)) {
            return Collections.emptyList();
        }
        
        // Get screen dimensions - using reflection to access private fields
        try {
            int x = getScreenX(screen);
            int y = getScreenY(screen);
            int backgroundWidth = getBackgroundWidth(screen);
            
            // Calculate button area (right side of the screen)
            // Our buttons are positioned at x + backgroundWidth + 4
            // Memory button: y + 20, size 60x20
            // Clear button: y + 44, size 60x20  
            // Persist button: y + 68, size 60x20
            // Plus some padding
            int buttonX = x + backgroundWidth + 4;
            int buttonY = y + 20;
            int buttonWidth = 64; // 60 + 4 padding
            int buttonHeight = 72; // Covers all three buttons (20+4+20+4+20+4)
            
            Rectangle exclusionZone = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
            return Collections.singletonList(exclusionZone);
            
        } catch (Exception e) {
            // If we can't get screen dimensions, return empty list
            return Collections.emptyList();
        }
    }
    
    /**
     * Check if this is a container screen we want to add memory buttons to
     */
    private boolean isContainerScreen(HandledScreen<?> screen) {
        String className = screen.getClass().getSimpleName();
        return className.contains("Chest") || 
               className.contains("Container") || 
               className.contains("Barrel") ||
               className.contains("Shulker");
    }
    
    /**
     * Get screen X position using reflection
     */
    private int getScreenX(HandledScreen<?> screen) {
        try {
            var field = HandledScreen.class.getDeclaredField("x");
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get screen Y position using reflection
     */
    private int getScreenY(HandledScreen<?> screen) {
        try {
            var field = HandledScreen.class.getDeclaredField("y");
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get background width using reflection
     */
    private int getBackgroundWidth(HandledScreen<?> screen) {
        try {
            var field = HandledScreen.class.getDeclaredField("backgroundWidth");
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (Exception e) {
            return 176; // Default chest width
        }
    }
}
