package bookwormpi.storagesense;

import bookwormpi.storagesense.client.keybind.KeyBindings;
import bookwormpi.storagesense.client.memory.EnhancedSBStyleMemoryManager;
import bookwormpi.storagesense.memory.ClientMemoryManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.text.Text;

public class StorageSenseClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Initialize keybinds
		KeyBindings.initialize();
		
		// Register debug commands
		registerCommands();
		
		// Load memory data when client starts
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			ClientMemoryManager.getInstance().loadMemoryData();
		});
		
		// Save memory data when client stops
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			ClientMemoryManager.getInstance().saveMemoryData();
		});
		
		StorageSense.LOGGER.info("Storage Sense client initialized!");
	}
	
	private void registerCommands() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			registerMemoryCommands(dispatcher);
		});
	}
	
	private void registerMemoryCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("storagesense")
			.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("memory")
				.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("stats")
					.executes(this::executeMemoryStats))
				.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("save")
					.executes(this::executeMemorySave))
				.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("toggle")
					.executes(this::executeMemoryToggle))));
	}
	
	private int executeMemoryStats(CommandContext<FabricClientCommandSource> context) {
		String stats = EnhancedSBStyleMemoryManager.getStats();
		context.getSource().sendFeedback(Text.literal("Memory Stats: " + stats));
		return 1;
	}
	
	private int executeMemorySave(CommandContext<FabricClientCommandSource> context) {
		EnhancedSBStyleMemoryManager.saveAll();
		context.getSource().sendFeedback(Text.literal("Memory data saved"));
		return 1;
	}
	
	private int executeMemoryToggle(CommandContext<FabricClientCommandSource> context) {
		boolean newMode = !EnhancedSBStyleMemoryManager.isPersistentMode();
		EnhancedSBStyleMemoryManager.setPersistentStorage(newMode);
		context.getSource().sendFeedback(Text.literal("Persistent memory " + (newMode ? "enabled" : "disabled")));
		return 1;
	}
}