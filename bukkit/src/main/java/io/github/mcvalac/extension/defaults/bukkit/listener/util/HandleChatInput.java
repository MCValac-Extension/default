package io.github.mcvalac.extension.defaults.bukkit.listener.util;

import io.github.mcvalac.mcbackpack.api.model.BackpackData;
import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.HandleInventoryOpen.BackpackHolder;
import io.github.mcvalac.extension.defaults.bukkit.manager.PasswordInputManager;
import io.github.mcvalac.extension.defaults.bukkit.util.InventorySerialization;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.Base64;

/**
 * Listens for chat events to handle password input for locked backpacks.
 * <p>
 * When a player attempts to open a locked backpack, they are placed in a pending state.
 * This listener intercepts their next chat message and validates the password through
 * the {@link MCBackpackProvider}. Verification is delegated to the provider (and thus the
 * active database backend) rather than performed locally, so it works uniformly across
 * the SQLite, MySQL, and remote API backends — including backends that intentionally
 * never expose the stored password hash to the client.
 * </p>
 */
public class HandleChatInput implements Listener {

    /** The main plugin instance, used for scheduling synchronous tasks. */
    private final Plugin plugin;

    /** The backpack provider used to verify passwords against the active backend. */
    private final MCBackpackProvider provider;

    /** The manager tracking players currently waiting to input a password. */
    private final PasswordInputManager passwordManager;

    /**
     * Constructs the chat input listener.
     *
     * @param plugin          The main plugin instance.
     * @param provider        The backpack provider used for password verification.
     * @param passwordManager The manager state for password inputs.
     */
    public HandleChatInput(Plugin plugin, MCBackpackProvider provider, PasswordInputManager passwordManager) {
        this.plugin = plugin;
        this.provider = provider;
        this.passwordManager = passwordManager;
    }

    /**
     * Intercepts chat messages from players in the password pending list.
     * <p>
     * If the player is pending, the event is cancelled (hiding the password from chat),
     * and the input is verified against the backpack via {@link MCBackpackProvider#checkPwd}.
     * </p>
     *
     * @param event The chat event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!passwordManager.isPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String input = event.getMessage();
        BackpackData data = passwordManager.getPending(player.getUniqueId());

        if (input.equalsIgnoreCase("cancel")) {
            passwordManager.removePending(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Backpack opening cancelled");
            return;
        }

        // Verify through the provider so the active backend performs the check.
        // This keeps remote backends (which never return the raw hash) working.
        provider.checkPwd(data.getUuid(), input).thenAccept(valid -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (Boolean.TRUE.equals(valid)) {
                    passwordManager.removePending(player.getUniqueId());

                    player.sendMessage(ChatColor.GREEN + "Password correct");

                    try {
                        String title = "Backpack";

                        Inventory backpackInv;
                        if (data.getContent() == null || data.getContent().isEmpty()) {
                            backpackInv = Bukkit.createInventory(new BackpackHolder(data.getUuid()), data.getSize(), title);
                        } else {
                            backpackInv = fromBase64(data.getContent(), data.getSize(), data.getUuid(), title);
                        }
                        player.openInventory(backpackInv);
                    } catch (Exception e) {
                        e.printStackTrace();
                        player.sendMessage(ChatColor.RED + "Could not open backpack");
                    }
                } else {
                    // Keep the player pending so they can retry.
                    player.sendMessage(ChatColor.RED + "Incorrect password " + ChatColor.RED + "Please try again");
                }
            });
        });
    }

    /**
     * Deserializes a Base64 string into a Bukkit Inventory.
     *
     * @param base64 The Base64 encoded inventory data.
     * @param size   The size of the inventory.
     * @param uuid   The UUID of the backpack owner/holder.
     * @param title  The inventory title.
     * @return The reconstructed Inventory object.
     * @throws IOException If IO errors occur.
     */
    private Inventory fromBase64(String base64, int size, String uuid, String title) throws IOException {
        Inventory inventory = Bukkit.createInventory(new BackpackHolder(uuid), size, title);
        if (base64 == null || base64.isEmpty()) return inventory;

        byte[] data = Base64.getDecoder().decode(base64);
        ItemStack[] items = InventorySerialization.fromBytes(data);

        inventory.setContents(items);
        return inventory;
    }
}
