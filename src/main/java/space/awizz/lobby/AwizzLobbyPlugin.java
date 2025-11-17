package space.awizz.lobby;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public final class AwizzLobbyPlugin extends JavaPlugin implements Listener, TabExecutor {

    private SelectorManager selectorManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        selectorManager = new SelectorManager(this);
        selectorManager.reload();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("selector") != null) {
            getCommand("selector").setExecutor(this);
            getCommand("selector").setTabCompleter(this);
        } else {
            getLogger().warning("Command 'selector' not defined in plugin.yml");
        }

        // Give selector to already online players (reload scenario)
        for (Player player : Bukkit.getOnlinePlayers()) {
            selectorManager.giveSelector(player);
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        selectorManager.giveSelector(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selectorManager.cleanupPlayer(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (selectorManager.isSelectorItem(dropped)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        if (!selectorManager.isSelectorItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        selectorManager.openMenu(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!selectorManager.isMenuInventory(event.getInventory())) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        SelectorManager.MenuEntry entry = selectorManager.getEntryAtSlot(event.getSlot());
        if (entry == null) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!entry.isEnabled()) {
            selectorManager.playDenySound(player);
            String denyMessage = entry.getDisabledMessage();
            if (denyMessage != null && !denyMessage.isEmpty()) {
                player.sendMessage(color(denyMessage));
            }
            return;
        }
        connectToServer(player, entry.getServer());
    }

    private void connectToServer(Player player, String server) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {
            out.writeUTF("Connect");
            out.writeUTF(server);
            player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
            String message = selectorManager.getConnectMessage(server);
            if (message != null && !message.isEmpty()) {
                player.sendMessage(color(message.replace("<server>", server)));
            }
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Unable to connect to " + server + ". Please try again.");
            getLogger().warning("Failed to send player " + player.getName() + " to " + server);
        }
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Command only for players.");
            return true;
        }
        Player player = (Player) sender;
        selectorManager.openMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

