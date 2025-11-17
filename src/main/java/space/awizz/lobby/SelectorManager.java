package space.awizz.lobby;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SelectorManager {

    private final AwizzLobbyPlugin plugin;

    private ItemStack selectorItem;
    private int selectorSlot;
    private SelectorMenu menu;
    private String connectMessage;
    private Sound denySound;

    public SelectorManager(AwizzLobbyPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        ConfigurationSection selectorSection = plugin.getConfig().getConfigurationSection("selector-item");
        selectorSlot = selectorSection.getInt("slot", 0);
        selectorItem = createItem(selectorSection);

        connectMessage = plugin.getConfig().getString("messages.connect");
        String denySoundName = plugin.getConfig().getString("messages.deny-sound", "BLOCK_NOTE_BASS");
        denySound = resolveSound(denySoundName, fallbackClickSound());

        ConfigurationSection menuSection = plugin.getConfig().getConfigurationSection("menu");
        menu = new SelectorMenu(
                ChatColor.translateAlternateColorCodes('&', menuSection.getString("title", "&8Selector")),
                menuSection.getInt("size", 9)
        );

        ConfigurationSection entriesSection = menuSection.getConfigurationSection("entries");
        if (entriesSection != null) {
            for (String key : entriesSection.getKeys(false)) {
                ConfigurationSection entrySec = entriesSection.getConfigurationSection(key);
                MenuEntry entry = new MenuEntry(
                        entrySec.getString("server"),
                        createItem(entrySec),
                        entrySec.getInt("slot"),
                        entrySec.getBoolean("enabled", true),
                        entrySec.getString("disabled-message")
                );
                menu.addEntry(entry);
            }
        }
    }

    public void giveSelector(Player player) {
        if (selectorItem == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getInventory().setItem(selectorSlot, selectorItem.clone());
            player.updateInventory();
        });
    }

    public void cleanupPlayer(Player player) {
        // No persistent state yet – reserved for future features
    }

    public boolean isSelectorItem(ItemStack stack) {
        if (stack == null || selectorItem == null) {
            return false;
        }
        if (stack.getType() != selectorItem.getType()) {
            return false;
        }
        if (!stack.hasItemMeta() || !selectorItem.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        ItemMeta selectorMeta = selectorItem.getItemMeta();
        return Objects.equals(meta.getDisplayName(), selectorMeta.getDisplayName());
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, menu.getSize(), menu.getTitle());
        for (MenuEntry entry : menu.getEntries()) {
            inv.setItem(entry.getSlot(), entry.getDisplayItem());
        }
        player.openInventory(inv);
    }

    public boolean isMenuInventory(Inventory inventory) {
        return inventory != null && menu.getTitle().equals(inventory.getTitle());
    }

    public MenuEntry getEntryAtSlot(int slot) {
        return menu.getEntry(slot);
    }

    public void playDenySound(Player player) {
        player.playSound(player.getLocation(), denySound, 1f, 1f);
    }

    public String getConnectMessage(String server) {
        return connectMessage;
    }

    private ItemStack createItem(ConfigurationSection section) {
        String materialName = section.getString("material", "COMPASS");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("Invalid material: " + materialName);
        }
        ItemStack stack = new ItemStack(material, section.getInt("amount", 1), (short) section.getInt("data", 0));
        ItemMeta meta = stack.getItemMeta();
        if (section.contains("name")) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("name")));
        }
        if (section.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private Sound resolveSound(String name, Sound fallback) {
        if (name == null) {
            return fallback;
        }
        Sound resolved = tryValueOf(name);
        if (resolved != null) {
            return resolved;
        }
        if ("NOTE_BASS".equalsIgnoreCase(name)) {
            resolved = tryValueOf("BLOCK_NOTE_BASS");
            if (resolved != null) {
                return resolved;
            }
        }
        if ("BLOCK_NOTE_BASS".equalsIgnoreCase(name)) {
            resolved = tryValueOf("NOTE_BASS");
            if (resolved != null) {
                return resolved;
            }
        }
        return fallback != null ? fallback : Sound.values()[0];
    }

    private Sound tryValueOf(String name) {
        if (name == null) {
            return null;
        }
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Sound fallbackClickSound() {
        Sound legacy = tryValueOf("CLICK");
        if (legacy != null) {
            return legacy;
        }
        Sound modern = tryValueOf("UI_BUTTON_CLICK");
        if (modern != null) {
            return modern;
        }
        return Sound.values()[0];
    }

    public static class SelectorMenu {
        private final String title;
        private final int size;
        private final Map<Integer, MenuEntry> entries = new HashMap<>();

        public SelectorMenu(String title, int size) {
            this.title = title;
            this.size = size;
        }

        public void addEntry(MenuEntry entry) {
            entries.put(entry.getSlot(), entry);
        }

        public String getTitle() {
            return title;
        }

        public int getSize() {
            return size;
        }

        public Collection<MenuEntry> getEntries() {
            return entries.values();
        }

        public MenuEntry getEntry(int slot) {
            return entries.get(slot);
        }
    }

    public static class MenuEntry {
        private final String server;
        private final ItemStack displayItem;
        private final int slot;
        private final boolean enabled;
        private final String disabledMessage;

        public MenuEntry(String server, ItemStack displayItem, int slot, boolean enabled, String disabledMessage) {
            this.server = server;
            this.displayItem = displayItem;
            this.slot = slot;
            this.enabled = enabled;
            this.disabledMessage = disabledMessage;
        }

        public String getServer() {
            return server;
        }

        public ItemStack getDisplayItem() {
            return displayItem.clone();
        }

        public int getSlot() {
            return slot;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getDisabledMessage() {
            return disabledMessage;
        }
    }
}

