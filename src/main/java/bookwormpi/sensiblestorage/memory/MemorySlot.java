package bookwormpi.sensiblestorage.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a memory configuration for a single inventory slot
 * Uses modern Fabric data component approach for persistence
 */
public record MemorySlot(Set<Item> allowedItems, boolean isConfigured) {
    
    public static final Codec<MemorySlot> CODEC = RecordCodecBuilder.create(builder -> {
        return builder.group(
            Codec.list(Registries.ITEM.getCodec())
                .xmap(HashSet::new, list -> new ArrayList<>(list))
                .fieldOf("allowed_items")
                .forGetter(slot -> new HashSet<>(slot.allowedItems)),
            Codec.BOOL.optionalFieldOf("configured", false)
                .forGetter(slot -> slot.isConfigured)
        ).apply(builder, MemorySlot::new);
    });
    
    // Default empty memory slot
    public static final MemorySlot EMPTY = new MemorySlot(new HashSet<>(), false);
    
    public MemorySlot() {
        this(new HashSet<>(), false);
    }
    
    public MemorySlot addAllowedItem(Item item) {
        Set<Item> newItems = new HashSet<>(this.allowedItems);
        newItems.add(item);
        return new MemorySlot(newItems, true);
    }
    
    public MemorySlot removeAllowedItem(Item item) {
        Set<Item> newItems = new HashSet<>(this.allowedItems);
        newItems.remove(item);
        boolean newConfigured = !newItems.isEmpty();
        return new MemorySlot(newItems, newConfigured);
    }
    
    public MemorySlot clearAllowedItems() {
        return new MemorySlot(new HashSet<>(), false);
    }
    
    public boolean canAcceptItem(ItemStack stack) {
        if (!isConfigured) {
            return true; // No memory configuration means any item is allowed
        }
        return allowedItems.contains(stack.getItem());
    }
    
    public List<Item> getAllowedItemsList() {
        return new ArrayList<>(allowedItems);
    }
}
