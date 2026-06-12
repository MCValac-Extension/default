package io.github.mcvalac.extension.defaults.bukkit.listener.util;

import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import io.github.mcvalac.extension.defaults.bukkit.manager.BackpackCooldownManager;
import io.github.mcvalac.extension.defaults.bukkit.manager.PasswordInputManager;
import io.github.mcvalac.extension.defaults.bukkit.util.InventorySerialization;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.Base64;

/**
 * Listens for player interactions to open backpacks.
 */
public class HandleInventoryOpen implements Listener {

    private final MCBackpackProvider provider;
    private final PasswordInputManager passwordManager;
    private final BackpackCooldownManager cooldownManager;
    private final NamespacedKey uuidKey;

    public HandleInventoryOpen(Plugin plugin, MCBackpackProvider provider, PasswordInputManager passwordManager, BackpackCooldownManager cooldownManager) {
        this.provider = provider;
        this.passwordManager = passwordManager;
        this.cooldownManager = cooldownManager;
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Player player = event.getPlayer();
        String backpackUuid;

        if (meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            backpackUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        } else {
            return;
        }

        event.setCancelled(true);

        if (cooldownManager.isOnCooldown(player.getUniqueId())) {
            long remainingMs = cooldownManager.getRemainingMillis(player.getUniqueId());
            long remainingSeconds = Math.max(1, (long) Math.ceil(remainingMs / 1000.0));
            player.sendMessage(ChatColor.YELLOW + "Please wait before opening your backpack again (" + remainingSeconds + "s)");
            return;
        }

        if (passwordManager.isPending(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Please enter your backpack password in chat");
            return;
        }

        player.sendMessage(ChatColor.GRAY + "Opening backpack...");

        // CHANGED: Pass player UUID to provider
        provider.open(backpackUuid, player.getUniqueId().toString()).thenAccept(data -> {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("MCBackpack"), () -> {

                if (data == null) {
                    player.sendMessage(ChatColor.RED + "Could not load backpack data");
                    return;
                }

                boolean hasPassword = (data.getPwdHash() != null && !data.getPwdHash().isEmpty());

                if (hasPassword && !player.isOp()) {
                    player.sendMessage(ChatColor.RED + "This backpack is locked");
                    player.sendMessage(ChatColor.YELLOW + "Type your password in chat, or type 'cancel' to abort");

                    passwordManager.addPending(player.getUniqueId(), data);
                    return;
                }

                try {
                    String title = "Backpack";

                    Inventory backpackInv;
                    if (data.getContent() == null || data.getContent().isEmpty()) {
                        backpackInv = Bukkit.createInventory(new BackpackHolder(backpackUuid), data.getSize(), title);
                    } else {
                        backpackInv = fromBase64(data.getContent(), data.getSize(), backpackUuid, title);
                    }

                    player.openInventory(backpackInv);

                } catch (IOException e) {
                    e.printStackTrace();
                    player.sendMessage(ChatColor.RED + "Failed to read backpack contents");
                }
            });
        });
    }

    private Inventory fromBase64(String base64, int size, String uuid, String title) throws IOException {
        Inventory inventory = Bukkit.createInventory(new BackpackHolder(uuid), size, title);

        if (base64 == null || base64.isEmpty()) return inventory;

        byte[] data = Base64.getDecoder().decode(base64);
        ItemStack[] items = InventorySerialization.fromBytes(data);

        inventory.setContents(items);
        return inventory;
    }

    public static class BackpackHolder implements org.bukkit.inventory.InventoryHolder {
        private final String uuid;
        public BackpackHolder(String uuid) { this.uuid = uuid; }
        public String getUuid() { return uuid; }
        @Override public Inventory getInventory() { return null; }
    }
}
