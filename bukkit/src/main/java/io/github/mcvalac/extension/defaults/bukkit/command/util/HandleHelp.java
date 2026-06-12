package io.github.mcvalac.extension.defaults.bukkit.command.util;

import io.github.mcvalac.extension.defaults.bukkit.command.MCBackpackCommandManager;
import io.github.mcvalac.extension.defaults.bukkit.command.IBackpackCommandHandle;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * Command handler that displays a help menu listing all available subcommands.
 */
public class HandleHelp implements IBackpackCommandHandle {

    /** The command manager used to retrieve the list of registered commands. */
    private final MCBackpackCommandManager manager;

    /**
     * Constructs the help command handler.
     *
     * @param manager The manager containing registered subcommands.
     */
    public HandleHelp(MCBackpackCommandManager manager) {
        this.manager = manager;
    }

    /**
     * Executes the help logic.
     * Generates a list of commands based on the player's permissions.
     *
     * @param sender The command sender.
     * @param args   Ignored.
     */
    @Override
    public void invoke(CommandSender sender, String[] args) {
        String header = ChatColor.GOLD + "" + ChatColor.BOLD + "MCBackpack Commands";
        sender.sendMessage(header);

        for (Map.Entry<String, IBackpackCommandHandle> entry : manager.getSubcommands().entrySet()) {
            String name = entry.getKey();
            IBackpackCommandHandle handle = entry.getValue();

            if (name.equalsIgnoreCase("help")) continue;

            if (handle.getPermission() == null || sender.hasPermission(handle.getPermission())) {
                String fullCommand = "/bp " + name;

                String cmdText = ChatColor.YELLOW + fullCommand + " ";
                String helpMsg = ChatColor.GRAY + handle.getHelp();

                sender.sendMessage(cmdText + helpMsg);
            }
        }
    }

    /**
     * Retrieves the help string for the help command.
     *
     * @return The usage syntax.
     */
    @Override
    public String getHelp() {
        return "- View this help menu";
    }

    /**
     * Retrieves the permission node for the help command.
     *
     * @return null, indicating no permission is required.
     */
    @Override
    public String getPermission() { return null; }
}
