package io.github.mcvalac.extension.defaults.bukkit.command.util;

import io.github.mcvalac.extension.defaults.bukkit.command.IBackpackCommandHandle;
import io.github.mcvalac.extension.defaults.bukkit.util.SkullTextures;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Base64;

/**
 * Command handler for giving the player a "Texture Applicator" item.
 * <p>
 * This command generates a special player head with a specific texture stored in its
 * persistent data container. When clicked on a valid backpack in the inventory,
 * the listener will apply this texture to the backpack.
 * </p>
 */
public class CHandleChangeTexture implements IBackpackCommandHandle {

    /** NamespacedKey used to store the target texture in the applicator item. */
    private final NamespacedKey applicatorKey;

    /**
     * Constructs a new texture change command handler.
     * @param plugin The main plugin instance.
     */
    public CHandleChangeTexture(Plugin plugin) {
        this.applicatorKey = new NamespacedKey(plugin, "backpack_texture_applicator");
    }

    /**
     * Executes the logic to give the applicator item.
     * @param sender The entity sending the command (must be a Player).
     * @param args   The arguments: [texture_base64].
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "/bp texture <base64_texture>");
            return;
        }

        String texture = args[0];

        // Create the Applicator Item
        ItemStack applicator = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) applicator.getItemMeta();

        if (meta != null) {
            // 1. Validate base64 to prevent crashes with invalid input
            try {
                new String(Base64.getDecoder().decode(texture));
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid texture value.");
                return;
            }

            // 2. Apply Texture (Visual)
            SkullTextures.apply(meta, texture);

            // 3. Set Name
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Backpack Texture Applicator");

            // 4. Store Data for Listener
            meta.getPersistentDataContainer().set(applicatorKey, PersistentDataType.STRING, texture);

            applicator.setItemMeta(meta);
        }

        // Give item
        var overflow = player.getInventory().addItem(applicator);
        if (!overflow.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), applicator);
        }

        player.sendMessage(ChatColor.GREEN + "Texture applicator received.");
    }

    /**
     * Retrieves the help text.
     * @return The usage syntax.
     */
    @Override
    public String getHelp() {
        return "<base64_texture> - Get a backpack texture applicator";
    }

    /**
     * Retrieves the permission node.
     * @return The permission string.
     */
    @Override
    public String getPermission() {
        return "mcbackpack.texture";
    }
}
