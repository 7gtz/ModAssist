# AutoChatMod - A Moderator's Assistant

[![Mod Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://www.curseforge.com/minecraft/mc-mods/autochatmod)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.4-green.svg)](https://www.minecraft.net)
[![Requires](https://img.shields.io/badge/Requires-Fabric-orange.svg)](https://fabricmc.net/)

AutoChatMod is a powerful client-side Minecraft mod designed to assist moderators. It automatically flags suspicious chat messages, detects spam, and streamlines the moderation process, allowing you to focus on the game while it keeps an eye on the chat for you.

---

## Table of Contents

- [Key Features](#key-features)
- [Installation](#installation)
- [Usage Workflow](#usage-workflow)
- [Configuration](#configuration)
- [Commands](#commands)
- [Default Keybinds](#default-keybinds)
- [Troubleshooting & FAQ](#troubleshooting--faq)
- [Reporting Issues](#reporting-issues)

---

## Key Features

* **Real-time Chat Monitoring**: Automatically detects spam, custom flagged terms (with typo/similarity detection), and exact flagged phrases.
* **X-Ray & Report Alerts**: specialized detection for server-side X-Ray logs and player reports, with distinct sounds and visual alerts.
* **Discord Webhook Integration**: Sends instant notifications to a designated Discord channel when a message is flagged, including the message content, the sender, and an optional ping for a user or role.
* **In-Game Action Overlay (HUD)**: A clean, draggable overlay appears when you click a player's name. It provides one-click access to common moderation commands.
* **Smart Nickname Resolution**: If a player using a nickname sends a flagged message, the mod automatically resolves their real username before you take action.
* **Click-to-Act Functionality**: All player messages in chat become clickable, allowing you to quickly open the Action HUD for any user, not just flagged ones.
* **Fully Configurable**: Every feature can be toggled and tweaked in-game via the ModMenu configuration screen. Customize flagged words, whitelists, spam sensitivity, and more.
* **Audible Alerts**: distinct sounds play for different events (Flagged Chat, X-Ray, Reports), ensuring you know exactly what requires attention.

---

## Installation

This is a **client-side** mod. It only needs to be installed on the moderator's computer, not on the server.

**Prerequisites:**

You must have the following installed. All files go into your `/.minecraft/mods/` folder.

1.  **Minecraft `1.21.4`**
2.  [Fabric Loader](https://fabricmc.net/use/installer/)
3.  [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
4.  [ModMenu](https://www.curseforge.com/minecraft/mc-mods/modmenu) (Required for in-game configuration)
5.  [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) (A dependency for the config screen)

**Steps:**

1.  Ensure all prerequisites are downloaded and placed in your `/.minecraft/mods/` folder.
2.  Download the latest `autochatmod-x.x.x.jar` file.
3.  Place the AutoChatMod JAR file into your `/.minecraft/mods/` folder.
4.  Launch Minecraft using the Fabric profile.

---

## Usage Workflow

Here's a typical moderation scenario using AutoChatMod:

1.  **Passive Monitoring**: Play the game as usual. The mod analyzes chat in the background.
2.  **Alert!**: A player types a message, or the server broadcasts an alert.
    *   **In-Game**: You will hear a distinct alert sound (customizable per event type), and the message will be highlighted in your chat (e.g., `[FLAGGED]`, `[X-RAY]`, `[REPORT]`).
    *   **On Discord**: A notification is instantly sent to your configured channel.
3.  **Take Action**:
    *   In your Minecraft chat, **click the player's username** in the flagged message.
    *   The **Action HUD** will appear. If the player was using a nickname, the mod automatically resolves their real name first.
4.  **Use the HUD**: With the HUD open for the target player, you can:
    *   Press **`X`** to teleport to them (`/tp <username>`).
    *   Press **`P`** to open your server's punishment GUI for them (`/punish <username>`).
    *   Press **`C`** to close the HUD.
    *   **Click and drag** the top of the HUD to move it anywhere on your screen.

---

## Configuration

To configure the mod, navigate to the main menu, click **Mods**, find **AutoChatMod** in the list, and click the gear icon (⚙️).

### General Settings

| Option                  | Description                                                              |
| :---------------------- | :----------------------------------------------------------------------- |
| `Enabled`                 | The master switch. Toggles the entire mod on or off.                     |
| w `Discord Webhook URL`     | Your Discord webhook URL. See below for setup instructions.              |
| `User Mention ID`         | The Discord User or Role ID to ping in alerts.                           |
| `Ping on Discord Alert`   | If enabled, the `User Mention ID` will be pinged in every alert message. |
| `Ignored System Usernames`| List of usernames/prefixes to completely ignore (e.g., specific bots).   |

### Detection Settings

| Option                 | Description                                                                                                        |
| :--------------------- | :----------------------------------------------------------------------------------------------------------------- |
| `Spam Detection`         | Enable or disable the spam detection module.                                                                       |
| `Term Detection`         | Enable or disable flagging messages based on individual words (e.g., slurs).                                     |
| `Phrase Detection`       | Enable or disable flagging messages based on exact phrases (e.g., "kill yourself").                                |
| `Similarity Threshold`   | A value from `0.0` to `1.0` (`0.8` recommended). Controls how similar a word must be to a flagged term to trigger an alert. This helps catch typos or bypass attempts (e.g., "byp@ss"). |

### X-Ray Settings
| Option                  | Description                                                              |
| :---------------------- | :----------------------------------------------------------------------- |
| `Alert Threshold`       | Number of ores found within the time window to trigger an alert (default: 4). |
| `Time Window`           | Time in seconds to accumulate ore finds for the threshold check (default: 10s). |
| `Alert Sound`           | Specific sound to play for X-Ray alerts.                                 |
| `Discord Ping`          | Whether to send a header ping to Discord for X-Ray alerts specifically.  |

### Report Settings
| Option                  | Description                                                              |
| :---------------------- | :----------------------------------------------------------------------- |
| `Alert Sound`           | Specific sound to play when a player report is detected.                 |
| `Discord Ping`          | Whether to send a header ping to Discord for Report alerts specifically. |

### Spam Detection Details

| Option                       | Description                                                                          |
| :--------------------------- | :----------------------------------------------------------------------------------- |
| `Spam Similarity Threshold`  | How similar two messages must be to be part of a spam sequence (`0.9` recommended).   |
| `Spam Message Count`         | The number of similar messages required within the time window to trigger a spam alert. |
| `Spam Time Window (seconds)` | The time frame in which the `Spam Message Count` must be reached.                    |
| `Spam Whitelist Prefixes`    | Prefixes to ignore for spam detection (e.g., server broadcasts like `[Broadcast]`).  |

### Flagged & Whitelisted Terms

* **Flagged Terms**: A list of individual words to watch for.
* **Whitelisted Terms**: Words that should never trigger an alert, even if they are similar to a flagged term (e.g., to prevent "bigger" from flagging a slur).
* **Flagged Phrases**: A list of exact, case-insensitive phrases to watch for.
* **Whitelisted Phrases**: Exact phrases that should be ignored.

### How to Get a Discord Webhook URL

1.  In your Discord server, go to `Server Settings` -> `Integrations`.
2.  Click `Webhooks` -> `New Webhook`.
3.  Give it a name (e.g., "MC Mod Alerts") and choose the channel for notifications.
4.  Click `Copy Webhook URL` and paste it into the mod's configuration field.

---

## Commands

The mod has one primary command for manual use:

* `/autochatmod action <username>`
    * Manually opens the Action HUD for the specified username.

---

## Default Keybinds

These can be changed in `Options -> Controls -> Key Binds` under the "AutoChatMod" category.

| Key | Action   | Description                                                           |
| :-- | :------- | :-------------------------------------------------------------------- |
| `X` | Teleport | When the Action HUD is open, teleports you to the target player.      |
| `P` | Punish   | When the Action HUD is open, opens the punish GUI for the target player. |
| `C` | Close    | Closes the Action HUD.                                                |

---

## Troubleshooting & FAQ

**Q: The mod isn't loading!**
**A:** Make sure you have installed the correct versions of Minecraft, Fabric Loader, Fabric API, ModMenu, and Cloth Config API. All are required.

**Q: My Discord webhook isn't working.**
**A:** Double-check that you have copied the entire URL correctly into the config. Test the webhook in Discord's settings to ensure it's active. Also, ensure the mod is enabled in the config.

**Q: The mod is flagging messages I don't want it to.**
**A:** The `Similarity Threshold` might be too low, or you may need to add words to the `Whitelisted Terms` list. For example, if "grape" is being flagged because it's similar to a flagged word, add "grape" to the whitelist.

---

## Reporting Issues

If you find a bug or have a feature request, please report it on our [GitHub Issues page](https://github.com/your-username/autochatmod/issues).