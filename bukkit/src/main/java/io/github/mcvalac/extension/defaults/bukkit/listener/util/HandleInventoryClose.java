package io.github.mcvalac.extension.defaults.bukkit.listener.util;

import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.HandleInventoryOpen.BackpackHolder;
import io.github.mcvalac.extension.defaults.bukkit.manager.BackpackCooldownManager;
import io.github.mcvalac.extension.defaults.bukkit.util.InventorySerialization;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.io.IOException;
import java.util.Base64;

/**
 * Listens for inventory close events to save backpack data.
 */
public class HandleInventoryClose implements Listener {

    private final MCBackpackProvider provider;
    private final BackpackCooldownManager cooldownManager;

    public HandleInventoryClose(MCBackpackProvider provider, BackpackCooldownManager cooldownManager) {
        this.provider = provider;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof BackpackHolder holder) {
            String uuid = holder.getUuid();

            try {
                String base64Content = toBase64(inventory);

                // CHANGED: Pass player UUID to provider
                provider.save(uuid, base64Content, event.getPlayer().getUniqueId().toString()).thenRun(() -> {
                    cooldownManager.markClosed(event.getPlayer().getUniqueId());
                });

            } catch (IOException e) {
                e.printStackTrace();
                event.getPlayer().sendMessage(ChatColor.RED + "Could not save backpack contents");
            }
        }
    }

    private String toBase64(Inventory inventory) throws IOException {
        byte[] data = InventorySerialization.toBytes(inventory.getContents());
        return Base64.getEncoder().encodeToString(data);
    }
}
