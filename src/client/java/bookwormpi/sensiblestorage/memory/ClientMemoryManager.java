package bookwormpi.sensiblestorage.memory;

import bookwormpi.sensiblestorage.SensibleStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Client-side memory manager that stores container memory configurations locally
 */
public class ClientMemoryManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MEMORY_FILE_NAME = "storage_sense_memory.json";
    
    // Map of container location to slot memories
    private final Map<ContainerLocation, Map<Integer, MemorySlot>> containerMemories = new HashMap<>();
    
    private static ClientMemoryManager INSTANCE;
    
    public static ClientMemoryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClientMemoryManager();
        }
        return INSTANCE;
    }
    
    /**
     * Represents a unique container location in the world
     */
    public record ContainerLocation(String dimensionId, BlockPos pos) {
        public static ContainerLocation of(World world, BlockPos pos) {
            return new ContainerLocation(world.getRegistryKey().getValue().toString(), pos);
        }
    }
    
    /**
     * JSON-serializable version of memory data
     */
    private static class SerializableMemoryData {
        Map<String, Map<Integer, SerializableMemorySlot>> containers = new HashMap<>();
        
        static class SerializableMemorySlot {
            Set<String> allowedItems = new HashSet<>();
            boolean isConfigured = false;
        }
    }
    
    /**
     * Set memory configuration for a specific slot in a container
     */
    public void setSlotMemory(World world, BlockPos pos, int slotIndex, MemorySlot memory) {
        ContainerLocation location = ContainerLocation.of(world, pos);
        containerMemories.computeIfAbsent(location, k -> new HashMap<>())
                        .put(slotIndex, memory);
        saveMemoryData();
    }
    
    /**
     * Get memory configuration for a specific slot
     */
    public MemorySlot getSlotMemory(World world, BlockPos pos, int slotIndex) {
        ContainerLocation location = ContainerLocation.of(world, pos);
        Map<Integer, MemorySlot> containerMem = containerMemories.get(location);
        if (containerMem == null) {
            return MemorySlot.EMPTY;
        }
        return containerMem.getOrDefault(slotIndex, MemorySlot.EMPTY);
    }
    
    /**
     * Check if a container has any memory configurations
     */
    public boolean hasMemoryConfiguration(World world, BlockPos pos) {
        ContainerLocation location = ContainerLocation.of(world, pos);
        Map<Integer, MemorySlot> containerMem = containerMemories.get(location);
        return containerMem != null && !containerMem.isEmpty() && 
               containerMem.values().stream().anyMatch(MemorySlot::isConfigured);
    }
    
    /**
     * Clear all memory for a container (when broken, etc.)
     */
    public void clearContainerMemory(World world, BlockPos pos) {
        ContainerLocation location = ContainerLocation.of(world, pos);
        containerMemories.remove(location);
        saveMemoryData();
    }
    
    /**
     * Load memory data from file
     */
    public void loadMemoryData() {
        try {
            Path memoryFile = getMemoryFilePath();
            if (!Files.exists(memoryFile)) {
                return;
            }
            
            String json = Files.readString(memoryFile);
            SerializableMemoryData data = GSON.fromJson(json, SerializableMemoryData.class);
            
            containerMemories.clear();
            for (Map.Entry<String, Map<Integer, SerializableMemoryData.SerializableMemorySlot>> containerEntry : data.containers.entrySet()) {
                String[] parts = containerEntry.getKey().split("@");
                if (parts.length != 2) continue;
                
                String dimensionId = parts[0];
                String[] posParts = parts[1].split(",");
                if (posParts.length != 3) continue;
                
                try {
                    BlockPos pos = new BlockPos(
                        Integer.parseInt(posParts[0]),
                        Integer.parseInt(posParts[1]),
                        Integer.parseInt(posParts[2])
                    );
                    ContainerLocation location = new ContainerLocation(dimensionId, pos);
                    
                    Map<Integer, MemorySlot> slotMemories = new HashMap<>();
                    for (Map.Entry<Integer, SerializableMemoryData.SerializableMemorySlot> slotEntry : containerEntry.getValue().entrySet()) {
                        SerializableMemoryData.SerializableMemorySlot serialSlot = slotEntry.getValue();
                        
                        Set<Item> allowedItems = new HashSet<>();
                        for (String itemId : serialSlot.allowedItems) {
                            Item item = Registries.ITEM.get(Identifier.of(itemId));
                            if (item != null) {
                                allowedItems.add(item);
                            }
                        }
                        
                        MemorySlot memorySlot = new MemorySlot(allowedItems, serialSlot.isConfigured);
                        slotMemories.put(slotEntry.getKey(), memorySlot);
                    }
                    
                    containerMemories.put(location, slotMemories);
                } catch (NumberFormatException e) {
                    SensibleStorage.LOGGER.warn("Failed to parse container position: " + containerEntry.getKey());
                }
            }
            
            SensibleStorage.LOGGER.info("Loaded memory configurations for {} containers", containerMemories.size());
        } catch (IOException e) {
            SensibleStorage.LOGGER.error("Failed to load memory data", e);
        }
    }
    
    /**
     * Save memory data to file
     */
    public void saveMemoryData() {
        try {
            SerializableMemoryData data = new SerializableMemoryData();
            
            for (Map.Entry<ContainerLocation, Map<Integer, MemorySlot>> containerEntry : containerMemories.entrySet()) {
                ContainerLocation location = containerEntry.getKey();
                String locationKey = location.dimensionId() + "@" + 
                                   location.pos().getX() + "," + 
                                   location.pos().getY() + "," + 
                                   location.pos().getZ();
                
                Map<Integer, SerializableMemoryData.SerializableMemorySlot> serialSlots = new HashMap<>();
                for (Map.Entry<Integer, MemorySlot> slotEntry : containerEntry.getValue().entrySet()) {
                    MemorySlot slot = slotEntry.getValue();
                    SerializableMemoryData.SerializableMemorySlot serialSlot = new SerializableMemoryData.SerializableMemorySlot();
                    serialSlot.isConfigured = slot.isConfigured();
                    
                    for (Item item : slot.allowedItems()) {
                        Identifier id = Registries.ITEM.getId(item);
                        serialSlot.allowedItems.add(id.toString());
                    }
                    
                    serialSlots.put(slotEntry.getKey(), serialSlot);
                }
                
                data.containers.put(locationKey, serialSlots);
            }
            
            Path memoryFile = getMemoryFilePath();
            Files.createDirectories(memoryFile.getParent());
            Files.writeString(memoryFile, GSON.toJson(data));
            
        } catch (IOException e) {
            SensibleStorage.LOGGER.error("Failed to save memory data", e);
        }
    }
    
    /**
     * Get the path to the memory data file
     */
    private Path getMemoryFilePath() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            // Integrated server
            return client.getServer().getSavePath(WorldSavePath.ROOT).resolve(MEMORY_FILE_NAME);
        } else if (client.world != null) {
            // Multiplayer - store in .minecraft/storage_sense/server_data/
            String serverName = client.getCurrentServerEntry() != null ? 
                              client.getCurrentServerEntry().address : "unknown_server";
            return client.runDirectory.toPath()
                  .resolve("storage_sense")
                  .resolve("server_data")
                  .resolve(serverName.replaceAll("[^a-zA-Z0-9._-]", "_"))
                  .resolve(MEMORY_FILE_NAME);
        } else {
            // Fallback
            return client.runDirectory.toPath().resolve("storage_sense").resolve(MEMORY_FILE_NAME);
        }
    }
}
