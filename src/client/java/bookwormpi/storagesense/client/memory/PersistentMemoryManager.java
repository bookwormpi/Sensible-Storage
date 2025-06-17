package bookwormpi.storagesense.client.memory;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent memory manager that saves/loads memory templates to/from disk
 * Inspired by ChestTracker's approach but simplified for our needs
 */
public class PersistentMemoryManager {
    private static final Path STORAGE_DIR = FabricLoader.getInstance().getGameDir().resolve("storagesense");
    private static final Path MEMORY_FILE = STORAGE_DIR.resolve("memory_templates.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // In-memory cache
    private static final Map<String, ContainerMemory> loadedMemories = new HashMap<>();
    private static boolean isLoaded = false;
    
    /**
     * Container memory data structure
     */
    public static class ContainerMemory {
        public ItemStack[] templates;
        public boolean memorizeMode;
        public long lastModified;
        
        public ContainerMemory() {
            this.templates = new ItemStack[0];
            this.memorizeMode = false;
            this.lastModified = System.currentTimeMillis();
        }
        
        public ContainerMemory(int slotCount) {
            this.templates = new ItemStack[slotCount];
            for (int i = 0; i < slotCount; i++) {
                templates[i] = ItemStack.EMPTY;
            }
            this.memorizeMode = false;
            this.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * Initialize the persistent memory system
     */
    public static void init() {
        try {
            Files.createDirectories(STORAGE_DIR);
            load();
        } catch (Exception e) {
        }
    }
    
    /**
     * Load memory templates from disk
     */
    public static void load() {
        if (isLoaded) return;
        
        try {
            if (!Files.exists(MEMORY_FILE)) {
                isLoaded = true;
                return;
            }
            
            String jsonData = Files.readString(MEMORY_FILE);
            JsonObject root = JsonParser.parseString(jsonData).getAsJsonObject();
            
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                String containerId = entry.getKey();
                JsonObject containerData = entry.getValue().getAsJsonObject();
                
                ContainerMemory memory = deserializeContainerMemory(containerData);
                if (memory != null) {
                    loadedMemories.put(containerId, memory);
                }
            }
            
            isLoaded = true;
            
        } catch (Exception e) {
            try {
                Files.move(MEMORY_FILE, MEMORY_FILE.resolveSibling("memory_templates.json.corrupt"), 
                          StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException backupError) {
            }
            isLoaded = true;
        }
    }
    
    /**
     * Save memory templates to disk
     */
    public static void save() {
        if (!isLoaded) return;
        
        try {
            JsonObject root = new JsonObject();
            
            for (Map.Entry<String, ContainerMemory> entry : loadedMemories.entrySet()) {
                String containerId = entry.getKey();
                ContainerMemory memory = entry.getValue();
                
                JsonObject containerData = serializeContainerMemory(memory);
                root.add(containerId, containerData);
            }
            
            Files.createDirectories(STORAGE_DIR);
            Files.writeString(MEMORY_FILE, GSON.toJson(root));
            
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Get or create memory for a container
     */
    public static ContainerMemory getOrCreateMemory(String containerId, int slotCount) {
        if (!isLoaded) load();
        
        return loadedMemories.computeIfAbsent(containerId, k -> new ContainerMemory(slotCount));
    }
    
    /**
     * Create a persistent container ID based on position and dimension
     */
    public static String createContainerIdFromPosition(String dimension, int x, int y, int z, String containerType) {
        return String.format("%s_%s_%d_%d_%d", containerType, dimension, x, y, z);
    }
    
    /**
     * Get memory templates for a container
     */
    public static ItemStack[] getMemoryTemplates(String containerId, int slotCount) {
        ContainerMemory memory = getOrCreateMemory(containerId, slotCount);
        
        // Resize if needed
        if (memory.templates.length != slotCount) {
            ItemStack[] newTemplates = new ItemStack[slotCount];
            int copyLength = Math.min(memory.templates.length, slotCount);
            System.arraycopy(memory.templates, 0, newTemplates, 0, copyLength);
            
            // Fill remaining with EMPTY
            for (int i = copyLength; i < slotCount; i++) {
                newTemplates[i] = ItemStack.EMPTY;
            }
            
            memory.templates = newTemplates;
            memory.lastModified = System.currentTimeMillis();
        }
        
        return memory.templates;
    }
    
    /**
     * Mark container memory as modified (for auto-save)
     */
    public static void markModified(String containerId) {
        ContainerMemory memory = loadedMemories.get(containerId);
        if (memory != null) {
            memory.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * Remove container memory (when explicitly cleared or container is removed)
     */
    public static void removeContainer(String containerId) {
        loadedMemories.remove(containerId);
        save(); // Immediately save when removing
    }
    
    /**
     * Get all loaded container IDs
     */
    public static java.util.Set<String> getLoadedContainerIds() {
        return loadedMemories.keySet();
    }
    
    /**
     * Serialize container memory to JSON
     */
    private static JsonObject serializeContainerMemory(ContainerMemory memory) {
        JsonObject obj = new JsonObject();
        
        // Serialize templates
        JsonArray templatesArray = new JsonArray();
        for (ItemStack stack : memory.templates) {
            JsonObject stackObj = serializeItemStack(stack);
            templatesArray.add(stackObj);
        }
        obj.add("templates", templatesArray);
        
        obj.addProperty("memorizeMode", memory.memorizeMode);
        obj.addProperty("lastModified", memory.lastModified);
        
        return obj;
    }
    
    /**
     * Deserialize container memory from JSON
     */
    private static ContainerMemory deserializeContainerMemory(JsonObject obj) {
        try {
            ContainerMemory memory = new ContainerMemory();
            
            // Deserialize templates
            JsonArray templatesArray = obj.getAsJsonArray("templates");
            memory.templates = new ItemStack[templatesArray.size()];
            
            for (int i = 0; i < templatesArray.size(); i++) {
                JsonObject stackObj = templatesArray.get(i).getAsJsonObject();
                memory.templates[i] = deserializeItemStack(stackObj);
            }
            
            memory.memorizeMode = obj.has("memorizeMode") ? obj.get("memorizeMode").getAsBoolean() : false;
            memory.lastModified = obj.has("lastModified") ? obj.get("lastModified").getAsLong() : System.currentTimeMillis();
            
            return memory;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Serialize ItemStack to JSON (simplified approach)
     */
    private static JsonObject serializeItemStack(ItemStack stack) {
        JsonObject obj = new JsonObject();
        
        if (stack.isEmpty()) {
            obj.addProperty("empty", true);
            return obj;
        }
        
        // Store item ID and count (without complex NBT for now)
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        obj.addProperty("item", itemId.toString());
        obj.addProperty("count", stack.getCount());
        
        // Note: For simplicity, we're not storing complex NBT data like enchantments
        // In a production system, you'd want to serialize the full ItemStack with all components
        
        return obj;
    }
    
    /**
     * Deserialize ItemStack from JSON
     */
    private static ItemStack deserializeItemStack(JsonObject obj) {
        try {
            if (obj.has("empty") && obj.get("empty").getAsBoolean()) {
                return ItemStack.EMPTY;
            }
            
            String itemIdStr = obj.get("item").getAsString();
            Identifier itemId = Identifier.tryParse(itemIdStr);
            if (itemId == null) {
                return ItemStack.EMPTY;
            }
            
            var item = Registries.ITEM.get(itemId);
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            
            return new ItemStack(item, count);
            
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
