package io.github.mcvalac.extension.defaults.bukkit.command.util;

import io.github.mcvalac.extension.defaults.bukkit.command.IBackpackCommandHandle;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Command handler for the "get" subcommand, delegating to sub-subcommands.
 */
public class CHandleSet implements IBackpackCommandHandle {

    private final IBackpackCommandHandle changeModelData;

    public CHandleSet(Plugin plugin) {
        this.changeModelData = new CHandleChangeModelData(plugin);
    }

    @Override
    public void invoke(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("model") && args[1].equalsIgnoreCase("data")) {
            String[] subArgs = (args.length <= 2) ? new String[0] : java.util.Arrays.copyOfRange(args, 2, args.length);
            changeModelData.invoke(sender, subArgs);
            return;
        }

        sender.sendMessage(ChatColor.RED + "/bp get model data <model_data>");
    }

    @Override
    public String getHelp() {
        return "model data <model_data> - Get set subcommands";
    }

    @Override
    public String getPermission() {
        return "mcbackpack.get";
    }
}
