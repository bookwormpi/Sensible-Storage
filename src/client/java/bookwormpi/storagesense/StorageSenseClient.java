package bookwormpi.storagesense;

import bookwormpi.storagesense.client.keybind.KeyBindings;
import bookwormpi.storagesense.memory.ClientMemoryManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class StorageSenseClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Initialize keybinds
		KeyBindings.initialize();
		
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
}