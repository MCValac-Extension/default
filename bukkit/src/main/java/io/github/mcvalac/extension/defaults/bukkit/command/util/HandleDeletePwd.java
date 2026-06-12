package io.github.mcvalac.extension.defaults.bukkit.command.util;

import io.github.mcvalac.extension.defaults.bukkit.command.IBackpackCommandHandle;
import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class HandleDeletePwd implements IBackpackCommandHandle {

    private final MCBackpackProvider provider;
    private final NamespacedKey uuidKey;

    public HandleDeletePwd(Plugin plugin, MCBackpackProvider provider) {
        this.provider = provider;
        this.uuidKey = new NamespacedKey(plugin, "backpack_uuid");
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        Player player = (Player) sender;
        boolean isOp = player.isOp();

        if (!isOp && args.length < 1) {
            player.sendMessage(ChatColor.RED + "/bp deletepwd <password>");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            player.sendMessage(ChatColor.RED + "You must hold a backpack in your main hand.");
            return;
        }

        String uuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        final String inputRaw = args.length > 0 ? args[0] : null;
        final String providedPwd = isOp ? null : inputRaw;

        // CHANGED: Pass player UUID to provider
        provider.open(uuid, player.getUniqueId().toString()).thenAccept(data -> {
            if (data == null) {
                player.sendMessage(ChatColor.RED + "Backpack not found.");
                return;
            }

            boolean hasPassword = (data.getPwdHash() != null && !data.getPwdHash().isEmpty()) || data.isLocked();

            if (!hasPassword) {
                player.sendMessage(ChatColor.YELLOW + "Backpack doesn't have a password yet.");
                return;
            }

            provider.deletePwd(uuid, providedPwd).thenAccept(success -> {
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "Backpack password removed.");
                } else {
                    player.sendMessage(ChatColor.RED + "Incorrect password");
                }
            });
        });
    }

    @Override
    public String getHelp() {
        return "<password> - Unlock held backpack";
    }

    @Override
    public String getPermission() { return null; }
}
