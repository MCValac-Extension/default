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

public class HandleSetPwd implements IBackpackCommandHandle {

    private final MCBackpackProvider provider;
    private final NamespacedKey uuidKey;

    public HandleSetPwd(Plugin plugin, MCBackpackProvider provider) {
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

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "/bp setpwd <password>");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.getPersistentDataContainer().has(uuidKey, PersistentDataType.STRING)) {
            player.sendMessage(ChatColor.RED + "You must hold a backpack in your main hand.");
            return;
        }

        String uuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        String rawPassword = args[0];

        // CHANGED: Pass player UUID to provider
        provider.open(uuid, player.getUniqueId().toString()).thenAccept(data -> {
            if (data == null) {
                player.sendMessage(ChatColor.RED + "Backpack not found.");
                return;
            }

            boolean hasPassword = (data.getPwdHash() != null && !data.getPwdHash().isEmpty()) || data.isLocked();

            if (hasPassword) {
                player.sendMessage(ChatColor.RED + "This backpack already has a password.");
                player.sendMessage(ChatColor.GRAY + "Use /bp changepwd or /bp deletepwd.");
                return;
            }

            provider.setPwd(uuid, rawPassword).thenRun(() -> {
                player.sendMessage(ChatColor.GREEN + "Backpack password set.");
            });
        });
    }

    @Override
    public String getHelp() {
        return "<password> - Set password for held backpack";
    }

    @Override
    public String getPermission() { return null; }
}
