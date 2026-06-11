package io.github.mcvalac.extension.defaults.papermc;

import io.github.mcengine.mcextension.api.IMCExtension;
import io.github.mcvalac.extension.defaults.bukkit.command.MCBackpackCommandManager;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.HandleChatInput;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.HandleInventoryClose;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.HandleInventoryOpen;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.HandleInventoryRestrict;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.LHandleChangeModelData;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.LHandleChangeTexture;
import io.github.mcvalac.extension.defaults.bukkit.listener.util.LHandleCreateBackpack;
import io.github.mcvalac.extension.defaults.bukkit.manager.BackpackCooldownManager;
import io.github.mcvalac.extension.defaults.bukkit.manager.PasswordInputManager;
import io.github.mcvalac.extension.defaults.bukkit.tabcompleter.MCBackpackTabCompleter;
import io.github.mcvalac.mcbackpack.common.MCBackpackProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * PaperMC entry point for the Default MCBackpack extension.
 * <p>
 * This class implements the latest {@link IMCExtension} lifecycle contract and wires
 * the shared, platform-independent backpack domain logic (commands, listeners, and
 * managers) into a running PaperMC server. The {@code /bp} command is registered
 * dynamically through the Paper command map, bypassing the need for a {@code plugin.yml}
 * entry.
 * </p>
 */
public class Default implements IMCExtension {

    /** Manager responsible for dispatching backpack subcommands. */
    private MCBackpackCommandManager commandManager;

    /** Manager tracking players currently entering a backpack password. */
    private PasswordInputManager passwordManager;

    /** Manager enforcing the backpack reopen cooldown window. */
    private BackpackCooldownManager cooldownManager;

    /** Tracks registered listeners so they can be unregistered on disable. */
    private final List<Listener> registeredListeners = new ArrayList<>();

    /**
     * Called when the extension is loaded by the host plugin.
     * <p>
     * Initializes managers, registers the event listeners, and dynamically registers
     * the {@code /bp} command. The host plugin instance is supplied as an opaque
     * {@link Object} by the extension manager and is expected to be a {@link JavaPlugin}.
     * </p>
     *
     * @param plugin   The host plugin instance (expected to be a {@link JavaPlugin}).
     * @param executor The executor for off-thread lifecycle work.
     */
    @Override
    public void onLoad(Object plugin, Executor executor) {
        if (!(plugin instanceof JavaPlugin javaPlugin)) {
            return;
        }

        MCBackpackProvider provider = MCBackpackProvider.getProvider();
        if (provider == null) {
            javaPlugin.getLogger().severe("MCBackpackProvider is not initialized. Extension cannot load.");
            return;
        }

        // Initialize managers
        this.passwordManager = new PasswordInputManager();
        this.cooldownManager = new BackpackCooldownManager(Duration.ofSeconds(2));
        this.commandManager = new MCBackpackCommandManager(javaPlugin, provider);

        // Register listeners and the /bp command
        registerListeners(javaPlugin, provider);
        registerCommand(javaPlugin);

        javaPlugin.getLogger().info("Default extension loaded successfully.");
    }

    /**
     * Registers all event listeners for this extension.
     *
     * @param plugin   The host plugin instance.
     * @param provider The MCBackpack provider instance.
     */
    private void registerListeners(JavaPlugin plugin, MCBackpackProvider provider) {
        registeredListeners.clear();

        registeredListeners.add(new HandleInventoryOpen(plugin, provider, passwordManager, cooldownManager));
        registeredListeners.add(new HandleInventoryClose(provider, cooldownManager));
        registeredListeners.add(new HandleChatInput(plugin, provider, passwordManager));
        registeredListeners.add(new HandleInventoryRestrict(plugin));
        registeredListeners.add(new LHandleChangeTexture(plugin, provider));
        registeredListeners.add(new LHandleChangeModelData(plugin));
        registeredListeners.add(new LHandleCreateBackpack(plugin, provider));

        for (Listener listener : registeredListeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    /**
     * Registers the {@code /bp} command dynamically into the Paper command map.
     *
     * @param plugin The host plugin instance.
     */
    private void registerCommand(JavaPlugin plugin) {
        MCBackpackTabCompleter tabCompleter = new MCBackpackTabCompleter(commandManager);

        Command cmd = new Command("bp", "MCBackpack commands", "/bp help", Collections.singletonList("backpack")) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                if (commandManager == null) {
                    sender.sendMessage(Component.text("MCBackpack command system is currently reloading or unavailable.").color(NamedTextColor.RED));
                    return true;
                }
                return commandManager.dispatch(sender, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                if (commandManager == null) {
                    return Collections.emptyList();
                }
                return tabCompleter.onTabComplete(sender, this, alias, args);
            }
        };

        plugin.getServer().getCommandMap().register(plugin.getName().toLowerCase(), cmd);
    }

    /**
     * Called when the extension is disabled.
     * <p>
     * Unregisters all listeners belonging to this extension to avoid duplicate
     * handlers on reload, and clears the manager references.
     * </p>
     *
     * @param plugin   The host plugin instance.
     * @param executor The executor for off-thread cleanup work.
     */
    @Override
    public void onDisable(Object plugin, Executor executor) {
        for (Listener listener : registeredListeners) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();

        this.commandManager = null;
        this.passwordManager = null;
        this.cooldownManager = null;

        if (plugin instanceof JavaPlugin javaPlugin) {
            javaPlugin.getLogger().info("Default extension has been disabled.");
        }
    }
}
