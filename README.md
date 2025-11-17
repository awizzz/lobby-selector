# Lobby Selector (AwizzLobby Plugin)

A Minecraft plugin for Spigot/Paper servers that provides an interactive server selector via a compass. Players can navigate between different BungeeCord servers through a fully configurable interface.

## ✨ Features

- **Integrated Server Selector**: Access via compass with configurable GUI  
- **BungeeCord Support**: Seamless connection to backend servers  
- **Configurable Menu**: Complete menu customization through configuration files  
- **Permission Management**: Access control via Bukkit's permission system  
- **Customizable Messages**: All messages can be modified in the config

## 🚀 Installation

1. Download the latest version from the Releases page (e.g. GitHub Releases).  
2. Place the JAR file in your server's `plugins/` folder.  
3. Restart (or reload) the server.  
4. Configure the plugin in `plugins/AwizzLobby/config.yml`.  
5. Grant the `awizzlobby.use` permission to players who should access the selector.

## ⚙️ Configuration

Example configuration in `config.yml`:

```yaml
menu:
  title: "&6Server Selector"
  servers:
    hub:
      name: "&aMain Hub"
      material: "COMPASS"
      lore:
        - "&7Join the main hub"
      bungee: "hub"
    survival:
      name: "&2Survival"
      material: "GRASS_BLOCK"
      lore:
        - "&7Survival game mode"
      bungee: "survival"

messages:
  no-permission: "&cYou don't have permission to use the lobby selector."
  connecting: "&eConnecting to %server%..."
Adjust material values to valid Material names for your server version. Add/remove servers under menu.servers as needed.

🎮 Usage
Players:

/lobby — Opens the server selection menu

Right-click with a compass — Opens the selector (if enabled)

Administrators:

Edit plugins/AwizzLobby/config.yml to add/remove servers and customize messages
