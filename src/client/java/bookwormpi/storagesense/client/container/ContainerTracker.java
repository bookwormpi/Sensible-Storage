package bookwormpi.storagesense.client.container;

import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Tracks container positions for open screen handlers
 * Inspired by Sophisticated Backpacks' container tracking approach
 */
public class ContainerTracker {
    private static final Map<ScreenHandler, ContainerInfo> CONTAINER_POSITIONS = new WeakHashMap<>();
    
    public record ContainerInfo(World world, BlockPos pos) {}
    
    /**
     * Register a container position when a screen is opened
     */
    public static void registerContainer(ScreenHandler handler, World world, BlockPos pos) {
        CONTAINER_POSITIONS.put(handler, new ContainerInfo(world, pos));
    }
    
    /**
     * Get the position of a container by its screen handler
     */
    public static ContainerInfo getContainerInfo(ScreenHandler handler) {
        return CONTAINER_POSITIONS.get(handler);
    }
    
    /**
     * Remove a container when it's closed
     */
    public static void unregisterContainer(ScreenHandler handler) {
        CONTAINER_POSITIONS.remove(handler);
    }
    
    /**
     * Try to determine container position from current screen
     */
    public static ContainerInfo getCurrentContainerInfo() {
        // This would need to be implemented based on the current screen
        // For now, return null as a placeholder
        return null;
    }
}
