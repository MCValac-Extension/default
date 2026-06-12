package io.github.mcvalac.extension.defaults.bukkit.command.util;

import io.github.mcvalac.extension.defaults.bukkit.command.IBackpackCommandHandle;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Command handler for giving the player a "Model Data Applicator" item.
 */
public class CHandleChangeModelData implements IBackpackCommandHandle {

    private final NamespacedKey applicatorKey;

    public CHandleChangeModelData(Plugin plugin) {
        this.applicatorKey = new NamespacedKey(plugin, "backpack_model_data_applicator");
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "/bp get model data <model_data>");
            return;
        }

        String modelData = String.join(" ", args);

        ItemStack applicator = new ItemStack(Material.PAPER);
        ItemMeta meta = applicator.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Backpack Model Data Applicator");
            meta.getPersistentDataContainer().set(applicatorKey, PersistentDataType.STRING, modelData);
            applicator.setItemMeta(meta);
        }

        var overflow = player.getInventory().addItem(applicator);
        if (!overflow.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), applicator);
        }

        player.sendMessage(ChatColor.GREEN + "Model data applicator received.");
    }

    @Override
    public String getHelp() {
        return "model data <model_data> - Get a model data applicator";
    }

    @Override
    public String getPermission() {
        return "mcbackpack.get";
    }
}
