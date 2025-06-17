package bookwormpi.sensiblestorage.memory;

import bookwormpi.sensiblestorage.SensibleStorage;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Data component that stores memory configurations for containers
 */
public record ContainerMemoryComponent(Map<Integer, MemorySlot> slotMemories) {
    
    public static final ComponentType<ContainerMemoryComponent> CONTAINER_MEMORY = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(SensibleStorage.MOD_ID, "container_memory"),
        ComponentType.<ContainerMemoryComponent>builder()
            .codec(ContainerMemoryComponent.CODEC)
            .build()
    );
    
    public static final com.mojang.serialization.Codec<ContainerMemoryComponent> CODEC = 
        com.mojang.serialization.codecs.RecordCodecBuilder.create(builder -> {
            return builder.group(
                com.mojang.serialization.Codec.unboundedMap(
                    com.mojang.serialization.Codec.INT, 
                    MemorySlot.CODEC
                ).fieldOf("slot_memories").forGetter(ContainerMemoryComponent::slotMemories)
            ).apply(builder, ContainerMemoryComponent::new);
        });
    
    // Empty component for new containers
    public static final ContainerMemoryComponent EMPTY = new ContainerMemoryComponent(Map.of());
    
    public ContainerMemoryComponent() {
        this(Map.of());
    }
    
    public ContainerMemoryComponent setSlotMemory(int slot, MemorySlot memory) {
        Map<Integer, MemorySlot> newMemories = new java.util.HashMap<>(this.slotMemories);
        if (memory.isConfigured()) {
            newMemories.put(slot, memory);
        } else {
            newMemories.remove(slot);
        }
        return new ContainerMemoryComponent(newMemories);
    }
    
    public MemorySlot getSlotMemory(int slot) {
        return slotMemories.getOrDefault(slot, MemorySlot.EMPTY);
    }
    
    public boolean hasMemoryForSlot(int slot) {
        return slotMemories.containsKey(slot) && slotMemories.get(slot).isConfigured();
    }
    
    public boolean isEmpty() {
        return slotMemories.isEmpty() || slotMemories.values().stream().noneMatch(MemorySlot::isConfigured);
    }
}
