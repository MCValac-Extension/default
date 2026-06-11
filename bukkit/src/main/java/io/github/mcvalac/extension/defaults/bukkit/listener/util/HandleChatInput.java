package io.github.mcvalac.extension.defaults.bukkit.listener.util;

import io.github.mcvalac.mcbackpack.api.model.BackpackData;
import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.HandleInventoryOpen.BackpackHolder;
import io.github.mcvalac.extension.defaults.bukkit.manager.PasswordInputManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!passwordManager.isPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText().serialize(event.message());
        BackpackData data = passwordManager.getPending(player.getUniqueId());

        if (input.equalsIgnoreCase("cancel")) {
            passwordManager.removePending(player.getUniqueId());
            Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.cancelled", "Backpack opening cancelled").color(NamedTextColor.YELLOW);
            player.sendMessage(msg);
            return;
        }

        // Verify through the provider so the active backend performs the check.
        // This keeps remote backends (which never return the raw hash) working.
        provider.checkPwd(data.getUuid(), input).thenAccept(valid -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (Boolean.TRUE.equals(valid)) {
                    passwordManager.removePending(player.getUniqueId());

                    Component msg = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.correct", "Password correct").color(NamedTextColor.GREEN);
                    player.sendMessage(msg);

                    try {
                        Component title = Component.translatable("mcvalac.mcbackpack.extension.default.gui.title", "Backpack");

                        Inventory backpackInv;
                        if (data.getContent() == null || data.getContent().isEmpty()) {
                            backpackInv = Bukkit.createInventory(new BackpackHolder(data.getUuid()), data.getSize(), title);
                        } else {
                            backpackInv = fromBase64(data.getContent(), data.getSize(), data.getUuid(), title);
                        }
                        player.openInventory(backpackInv);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Component err = Component.translatable("mcvalac.mcbackpack.extension.default.msg.error.item.open", "Could not open backpack").color(NamedTextColor.RED);
                        player.sendMessage(err);
                    }
                } else {
                    // Keep the player pending so they can retry.
                    Component incorrect = Component.translatable("mcvalac.mcbackpack.extension.default.msg.password.incorrect", "Incorrect password").color(NamedTextColor.RED);
                    Component tryAgain = Component.translatable("mcvalac.mcbackpack.extension.default.msg.try.again", "Please try again").color(NamedTextColor.RED);

                    Component builder = incorrect.append(Component.text(" ")).append(tryAgain);
                    player.sendMessage(builder);
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
    private Inventory fromBase64(String base64, int size, String uuid, Component title) throws IOException {
        Inventory inventory = Bukkit.createInventory(new BackpackHolder(uuid), size, title);
        if (base64 == null || base64.isEmpty()) return inventory;

        byte[] data = Base64.getDecoder().decode(base64);
        ItemStack[] items = ItemStack.deserializeItemsFromBytes(data);

        inventory.setContents(items);
        return inventory;
    }
}
