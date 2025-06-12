package bookwormpi.storagesense.client.gui;

import bookwormpi.storagesense.memory.ClientMemoryManager;
import bookwormpi.storagesense.memory.MemorySlot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI for configuring memory slots in containers
 * 
 * Enhanced with patterns inspired by Sophisticated Backpacks' GUI design (GPL-3.0)
 * Credit: Based on UI patterns from Salandora/SophisticatedBackpacks
 * https://github.com/Salandora/SophisticatedBackpacks
 */
public class MemoryConfigScreen extends Screen {
    private final World world;
    private final BlockPos containerPos;
    private final int slotIndex;
    private final Screen parentScreen;
    
    private MemorySlot currentMemory;
    private List<Item> allowedItems;
    private List<Item> filteredAvailableItems;
    private int scrollOffset = 0;
    private TextFieldWidget searchField;
    
    private static final int ITEMS_PER_ROW = 9;
    private static final int VISIBLE_ROWS = 6;
    private static final int ITEM_SIZE = 18;
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 240;
    
    // Colors inspired by Sophisticated Backpacks
    private static final int PANEL_COLOR = 0xF0383838;
    private static final int HIGHLIGHT_ADD = 0x8000FF00;
    private static final int HIGHLIGHT_REMOVE = 0x80FF0000;
    private static final int BORDER_COLOR = 0xFF555555;
    
    public MemoryConfigScreen(World world, BlockPos containerPos, int slotIndex, Screen parentScreen) {
        super(Text.translatable("storagesense.gui.memory_config.title"));
        this.world = world;
        this.containerPos = containerPos;
        this.slotIndex = slotIndex;
        this.parentScreen = parentScreen;
        
        this.currentMemory = ClientMemoryManager.getInstance().getSlotMemory(world, containerPos, slotIndex);
        this.allowedItems = new ArrayList<>(currentMemory.allowedItems());
        this.updateFilteredItems("");
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Search field
        this.searchField = new TextFieldWidget(this.textRenderer, centerX - 75, centerY - 10, 150, 18, Text.translatable("storagesense.gui.memory_config.search"));
        this.searchField.setChangedListener(this::updateFilteredItems);
        this.searchField.setPlaceholder(Text.translatable("storagesense.gui.memory_config.search_placeholder"));
        this.addSelectableChild(this.searchField);
        
        // Action buttons with improved spacing
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("storagesense.gui.memory_config.clear_all"),
            button -> {
                allowedItems.clear();
                saveMemoryConfiguration();
            }
        ).dimensions(centerX - 100, centerY + 95, 60, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("storagesense.gui.memory_config.save"),
            button -> {
                saveMemoryConfiguration();
                this.close();
            }
        ).dimensions(centerX - 35, centerY + 95, 70, 20).build());
        
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.cancel"),
            button -> this.close()
        ).dimensions(centerX + 40, centerY + 95, 60, 20).build());
        
        this.setInitialFocus(this.searchField);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw background
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Draw main panel background
        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = centerY - PANEL_HEIGHT / 2;
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_COLOR);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, BORDER_COLOR);
        
        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, panelY + 10, 0xFFFFFF);
        
        // Draw slot index info
        Text slotInfo = Text.translatable("storagesense.gui.memory_config.slot", slotIndex);
        context.drawCenteredTextWithShadow(this.textRenderer, slotInfo, centerX, panelY + 25, 0xAAAAAA);
        
        // Draw current allowed items
        renderAllowedItems(context, centerX, centerY, mouseX, mouseY);
        
        // Draw search field
        this.searchField.render(context, mouseX, mouseY, delta);
        
        // Draw available items for selection
        renderAvailableItems(context, centerX, centerY, mouseX, mouseY);
        
        // Draw usage instructions
        renderInstructions(context, centerX, centerY);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderAllowedItems(DrawContext context, int centerX, int centerY, int mouseX, int mouseY) {
        Text label = Text.translatable("storagesense.gui.memory_config.allowed_items");
        context.drawTextWithShadow(this.textRenderer, label, centerX - 90, centerY - 85, 0xFFFFFF);
        
        int startX = centerX - 81; // 9 items * 18 pixels / 2
        int startY = centerY - 65;
        
        // Draw allowed items background
        context.fill(startX - 2, startY - 2, startX + (ITEMS_PER_ROW * ITEM_SIZE) + 2, startY + 20, 0x40000000);
        
        for (int i = 0; i < Math.min(allowedItems.size(), ITEMS_PER_ROW); i++) {
            int x = startX + i * ITEM_SIZE;
            int y = startY;
            
            Item item = allowedItems.get(i);
            ItemStack stack = new ItemStack(item);
            
            // Draw slot background
            context.fill(x, y, x + 16, y + 16, 0x80333333);
            context.drawBorder(x, y, 16, 16, 0xFF666666);
            
            // Draw item
            context.drawItem(stack, x, y);
            
            // Check for hover/click to remove
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                context.fill(x, y, x + 16, y + 16, HIGHLIGHT_REMOVE);
                
                // Show tooltip
                if (this.client != null) {
                    List<Text> tooltip = List.of(
                        stack.getName(),
                        Text.translatable("storagesense.gui.memory_config.click_to_remove")
                    );
                    context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                }
            }
        }
        
        // Show count if there are more items than can be displayed
        if (allowedItems.size() > ITEMS_PER_ROW) {
            Text count = Text.literal("+" + (allowedItems.size() - ITEMS_PER_ROW));
            context.drawTextWithShadow(this.textRenderer, count, startX + (ITEMS_PER_ROW * ITEM_SIZE) + 5, startY + 4, 0xAAAAAA);
        }
    }
    
    private void renderAvailableItems(DrawContext context, int centerX, int centerY, int mouseX, int mouseY) {
        Text label = Text.translatable("storagesense.gui.memory_config.available_items");
        context.drawTextWithShadow(this.textRenderer, label, centerX - 90, centerY + 15, 0xFFFFFF);
        
        int startX = centerX - 81;
        int startY = centerY + 35;
        
        // Draw available items background
        int areaWidth = ITEMS_PER_ROW * ITEM_SIZE;
        int areaHeight = VISIBLE_ROWS * ITEM_SIZE;
        context.fill(startX - 2, startY - 2, startX + areaWidth + 2, startY + areaHeight + 2, 0x40000000);
        
        int visibleStart = scrollOffset * ITEMS_PER_ROW;
        int visibleEnd = Math.min(visibleStart + (VISIBLE_ROWS * ITEMS_PER_ROW), filteredAvailableItems.size());
        
        for (int i = visibleStart; i < visibleEnd; i++) {
            int displayIndex = i - visibleStart;
            int row = displayIndex / ITEMS_PER_ROW;
            int col = displayIndex % ITEMS_PER_ROW;
            
            int x = startX + col * ITEM_SIZE;
            int y = startY + row * ITEM_SIZE;
            
            Item item = filteredAvailableItems.get(i);
            ItemStack stack = new ItemStack(item);
            
            // Draw slot background
            context.fill(x, y, x + 16, y + 16, 0x80333333);
            context.drawBorder(x, y, 16, 16, 0xFF666666);
            
            // Draw item
            context.drawItem(stack, x, y);
            
            // Check for hover/click to add
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                boolean alreadyAllowed = allowedItems.contains(item);
                context.fill(x, y, x + 16, y + 16, alreadyAllowed ? 0x80FFFF00 : HIGHLIGHT_ADD);
                
                // Show tooltip
                if (this.client != null) {
                    List<Text> tooltip = new ArrayList<>();
                    tooltip.add(stack.getName());
                    if (alreadyAllowed) {
                        tooltip.add(Text.translatable("storagesense.gui.memory_config.already_allowed"));
                    } else {
                        tooltip.add(Text.translatable("storagesense.gui.memory_config.click_to_add"));
                    }
                    context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                }
            }
        }
        
        // Draw scroll indicator if needed
        if (filteredAvailableItems.size() > VISIBLE_ROWS * ITEMS_PER_ROW) {
            int maxScroll = (filteredAvailableItems.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW - VISIBLE_ROWS;
            int scrollbarHeight = areaHeight * VISIBLE_ROWS / ((filteredAvailableItems.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW);
            int scrollbarY = startY + (areaHeight - scrollbarHeight) * scrollOffset / Math.max(1, maxScroll);
            
            context.fill(startX + areaWidth + 4, startY, startX + areaWidth + 8, startY + areaHeight, 0x80000000);
            context.fill(startX + areaWidth + 4, scrollbarY, startX + areaWidth + 8, scrollbarY + scrollbarHeight, 0xFFAAAAAA);
        }
    }
    
    private void renderInstructions(DrawContext context, int centerX, int centerY) {
        List<Text> instructions = List.of(
            Text.translatable("storagesense.gui.memory_config.instruction1"),
            Text.translatable("storagesense.gui.memory_config.instruction2")
        );
        
        int y = centerY + 75;
        for (Text instruction : instructions) {
            context.drawCenteredTextWithShadow(this.textRenderer, instruction, centerX, y, 0x888888);
            y += 10;
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle allowed items click (remove)
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - 81;
        int startY = centerY - 65;
        
        for (int i = 0; i < Math.min(allowedItems.size(), ITEMS_PER_ROW); i++) {
            int x = startX + i * ITEM_SIZE;
            int y = startY;
            
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                allowedItems.remove(i);
                return true;
            }
        }
        
        // Handle available items click (add)
        startX = centerX - 81;
        startY = centerY + 35;
        
        int visibleStart = scrollOffset * ITEMS_PER_ROW;
        int visibleEnd = Math.min(visibleStart + (VISIBLE_ROWS * ITEMS_PER_ROW), filteredAvailableItems.size());
        
        for (int i = visibleStart; i < visibleEnd; i++) {
            int displayIndex = i - visibleStart;
            int row = displayIndex / ITEMS_PER_ROW;
            int col = displayIndex % ITEMS_PER_ROW;
            
            int x = startX + col * ITEM_SIZE;
            int y = startY + row * ITEM_SIZE;
            
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                Item item = filteredAvailableItems.get(i);
                if (!allowedItems.contains(item)) {
                    allowedItems.add(item);
                }
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Handle scrolling in the available items area
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - 81;
        int endX = startX + (ITEMS_PER_ROW * ITEM_SIZE);
        int startY = centerY + 35;
        int endY = startY + (VISIBLE_ROWS * ITEM_SIZE);
        
        if (mouseX >= startX && mouseX <= endX && mouseY >= startY && mouseY <= endY) {
            int maxScroll = Math.max(0, (filteredAvailableItems.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW - VISIBLE_ROWS);
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) verticalAmount));
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    private void updateFilteredItems(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            filteredAvailableItems = new ArrayList<>();
            Registries.ITEM.forEach(item -> {
                if (item != Items.AIR) {
                    filteredAvailableItems.add(item);
                }
            });
        } else {
            String lowerSearch = searchText.toLowerCase();
            filteredAvailableItems = Registries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .filter(item -> {
                    String itemName = new ItemStack(item).getName().getString().toLowerCase();
                    String itemId = Registries.ITEM.getId(item).toString().toLowerCase();
                    return itemName.contains(lowerSearch) || itemId.contains(lowerSearch);
                })
                .collect(Collectors.toList());
        }
        
        scrollOffset = 0; // Reset scroll when filter changes
    }
    
    private void saveMemoryConfiguration() {
        MemorySlot newMemory = new MemorySlot(
            new java.util.HashSet<>(allowedItems),
            !allowedItems.isEmpty()
        );
        ClientMemoryManager.getInstance().setSlotMemory(world, containerPos, slotIndex, newMemory);
    }
    
    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parentScreen);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
