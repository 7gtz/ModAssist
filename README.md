# ModAssist - A Moderator's Assistant

[![Mod Version](https://img.shields.io/badge/version-2.1.0-blue.svg)](https://github.com/your-username/modassist)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.10-green.svg)](https://www.minecraft.net)
[![Requires](https://img.shields.io/badge/Requires-Fabric-orange.svg)](https://fabricmc.net/)

ModAssist is a powerful client-side Minecraft mod designed for server moderators. It monitors chat in real time, flags suspicious messages, detects spam, X-Ray activity, and player reports — then gives you one-click access to moderation commands. Pairs with an optional Discord bot for rich alerts and a live staff dashboard.

---

## Table of Contents

- [Key Features](#key-features)
- [Installation](#installation)
- [Discord Bot Setup](#discord-bot-setup)
- [Usage Workflow](#usage-workflow)
- [Configuration](#configuration)
- [Commands](#commands)
- [Default Keybinds](#default-keybinds)
- [Troubleshooting & FAQ](#troubleshooting--faq)
- [Reporting Issues](#reporting-issues)

---

## Key Features

* **Real-time Chat Monitoring**: Detects spam, flagged terms (with typo/similarity detection), and exact flagged phrases.
* **X-Ray & Report Alerts**: Specialized detection for server-side X-Ray logs and player reports, each with distinct sounds and visual indicators.
* **Discord Bot Integration**: Sends structured alert embeds to per-type Discord channels (X-Ray, Spam, Report, Flagged) via a custom bot API, with grouped/editable messages and deduplication.
* **Staff Activity Dashboard**: A live-updating Discord embed that shows all online staff with their Minecraft skin, Discord mention, and session duration.
* **Heartbeat System**: The mod sends periodic heartbeats to the bot. If a moderator crashes or closes their game without logging out, the bot automatically detects the timeout and removes them from the online staff list.
* **In-Game Action Overlay (HUD)**: A draggable, resizable glassmorphism-styled overlay with one-key access to teleport, punish, and custom commands.
* **Auto-Teleport on Server Join**: When teleporting to an offline player (cross-server), the mod automatically sends `/tp` after arriving on their server.
* **Smart Nickname Resolution**: Automatically resolves nicknames to real usernames before executing moderation actions.
* **Click-to-Act Chat**: All player messages become clickable — click any username to open the Action HUD.
* **Customizable Action Commands**: Three configurable command slots with custom labels, all using `{player}` as a placeholder.
* **Per-Event Sound Alerts**: Distinct, configurable sounds for Chat Flags, X-Ray alerts, and Reports.
* **Evidence Screenshots**: Automatically captures a screenshot when you mute a player, saved with the player's name and reason.
* **Fully Configurable**: Every feature is toggleable and customizable in-game via the ModMenu settings screen.

---

## Installation

This is a **client-side** mod. It only needs to be installed on the moderator's computer, not on the server.

**Prerequisites:**

All files go into your `/.minecraft/mods/` folder.

1. **Minecraft `1.21.10`**
2. [Fabric Loader](https://fabricmc.net/use/installer/)
3. [Fabric API](https://modrinth.com/mod/fabric-api)
4. [ModMenu](https://modrinth.com/mod/modmenu) (Required for in-game configuration)
5. [Cloth Config API](https://modrinth.com/mod/cloth-config) (Dependency for the config screen)

**Steps:**

1. Ensure all prerequisites are downloaded and placed in your `/.minecraft/mods/` folder.
2. Download the latest `modassist-x.x.x.jar` file.
3. Place it into your `/.minecraft/mods/` folder.
4. Launch Minecraft using the Fabric profile.

---

**Staff Dashboard:**

Once the Status Dashboard channel is set, the bot posts and continuously edits a single message showing all online staff — including their Minecraft skin face, Discord mention, and session duration. The dashboard updates on login, logout, and heartbeat timeout.

---

## Usage Workflow

1. **Passive Monitoring**: Play the game as normal. The mod analyzes chat in the background and sends heartbeats to the Discord bot every 60 seconds.
2. **Alert!**: A player triggers a detection rule.
   - **In-Game**: A distinct alert sound plays (customizable per event type), and the message is highlighted in chat with a tag like `[SPAM]`, `[FLAGGED]`, or via the Action HUD.
   - **On Discord**: A rich embed is sent to the appropriate channel. Online staff are pinged. Repeated alerts from the same player are grouped into a single updating embed.
3. **Take Action**:
   - **Click** the player's username in the flagged chat message, or
   - **Look at a player** and press `G` to select them directly.
   - The **Action HUD** appears with the target's name and flag type.
4. **Use the HUD**:
   - Press **`X`** → Teleport to the player. If they're on another server, the mod runs `/goto` first, then auto-teleports after you arrive.
   - Press **`P`** → Open the punishment GUI (`/punish`). For spam, this can be set to instant-punish mode.
   - Press **`L`** → Run Command #1.
   - Press **`H`** → Run Command #2.
   - Press **`9`** → Run Command #3.
   - Press **`C`** → Close the HUD.
   - **Click and drag** the HUD to reposition it. **Drag the corner** to resize.

---

## Configuration

Open the config via the main menu → **Mods** → **ModAssist** → ⚙️ gear icon.

### General

| Option | Description |
| :--- | :--- |
| `Enabled` | Master switch for the entire mod. |
| `Discord Webhook URL` | Standard Discord webhook URL (used when Custom Bot is off). |
| `Use Custom Bot API` | Send structured JSON payloads to the custom bot instead of plain webhook messages. |
| `Custom Bot URL` | Base URL of your deployed Discord bot (e.g., `http://your-host:3242`). |
| `User Mention ID` | Your Discord User ID, used for pinging in alerts. |
| `Ping on Discord Alert` | Whether to include your mention in Discord alert messages. |

### Detection

| Option | Description |
| :--- | :--- |
| `Auto-Open Overlay on Flag` | Automatically show the Action HUD when a message is flagged. |
| `Auto-Open Punish GUI on Flag` | Automatically run `/punish` when a message is flagged (requires overlay to be on). |
| `Instant Punish for Spam` | Pressing Punish on a spammer runs `/punish <user> i:1` and closes the overlay instantly. |
| `Spam Detection` | Toggle the spam detection module. |
| `Term Detection` | Toggle flagging by individual words. |
| `Phrase Detection` | Toggle flagging by exact phrases. |
| `Similarity Threshold` | How similar a word must be to a flagged term to trigger (0.0–1.0, recommended: 0.8). |

### Action Commands

All commands use `{player}` as a placeholder for the target username. Commands are sent without a leading `/`.

| Option |
| :--- |
| `Command #1 Label` / `Command` |
| `Command #2 Label` / `Command` |
| `Command #3 Label` / `Command` |

### Spam Detection

| Option | Description |
| :--- | :--- |
| `Spam Similarity Threshold` | How similar messages must be to count as a spam sequence (recommended: 0.9). |
| `Spam Message Count` | Number of similar messages within the time window to trigger (default: 3). |
| `Spam Time Window` | Time frame in seconds for counting similar messages (default: 15). |
| `Spam Whitelist Prefixes` | Messages starting with these prefixes won't trigger spam detection. |

### X-Ray Detection

| Option | Description |
| :--- | :--- |
| `Alert Threshold` | Number of ore finds within the time window to trigger (default: 4). |
| `Time Window` | Seconds to accumulate ore finds (default: 10). |

### Sounds

| Option | Description |
| :--- | :--- |
| `Chat Alert Sound` | Sound for flagged chat messages. |
| `X-Ray Alert Sound` | Sound for X-Ray alerts. |
| `Report Alert Sound` | Sound for player reports. |
| `Volume` / `Pitch` | Adjustable from 0.0 to 2.0. |
| Per-event enable toggles | Each sound type can be individually enabled/disabled. |

### Evidence

| Option | Description |
| :--- | :--- |
| `Enable Evidence Screenshots` | Auto-screenshot when you mute a player. |

---

## Commands

| Command | Description |
| :--- | :--- |
| `/autochatmod action <username>` | Manually open the Action HUD for a player. |
| `/autochatmod testscreenshot` | Test the evidence screenshot functionality. |

---

## Default Keybinds

These can be changed in `Options → Controls → Key Binds` under the **ModAssist** category.

| Key | Action | Context | Description |
| :-- | :--- | :--- | :--- |
| `G` | Select Player | Always | Look at a player and press to open the Action HUD for them. |
| `X` | Teleport | HUD open | Teleport to the target. Auto-TPs after cross-server `/goto`. |
| `P` | Punish | HUD open | Open punishment GUI (or instant-punish for spam). |
| `L` | Command #1 | HUD open | Run configurable command (default: Check Alts). |
| `H` | Command #2 | HUD open | Run configurable command (default: Check Fly). |
| `9` | Command #3 | HUD open (Report) | Run configurable command (default: Approve Report). |
| `C` | Close | HUD open | Close the Action HUD. |

---

## Troubleshooting & FAQ

**Q: The mod isn't loading!**
**A:** Ensure you have the correct versions of Minecraft, Fabric Loader, Fabric API, ModMenu, and Cloth Config API installed. All are required.

**Q: My Discord bot isn't receiving alerts.**
**A:** Check that **Use Custom Bot API** is enabled, the **Custom Bot URL** is correct and reachable, and **Ping on Discord Alert** is on. Verify the bot is running by checking its console output.

**Q: The staff dashboard isn't updating.**
**A:** Make sure you've used `/setchannel type:Status Dashboard channel:#your-channel` in Discord. The dashboard only updates when staff log in, log out, or the heartbeat times out.

**Q: A staff member still shows as online after they disconnected.**
**A:** The heartbeat timeout is 90 seconds. After the staff member's client stops sending heartbeats, they'll be removed automatically within ~90 seconds. If the bot itself was restarted, the first heartbeat from an active client will re-register them.

**Q: The mod is flagging messages I don't want it to.**
**A:** The `Similarity Threshold` may be too low, or you need to add words to the `Whitelisted Terms` list. For example, if "grape" is being flagged because it's similar to a flagged word, add "grape" to the whitelist.

**Q: The auto-teleport after `/goto` isn't working.**
**A:** The auto-TP only triggers when the target was initially detected as offline (not in tab list). After running `/goto`, the mod waits 1 second and checks if the player appeared in the tab list. If the server switch takes longer than 1 second, the auto-TP won't trigger — use `X` again manually.

---

## Reporting Issues

If you find a bug or have a feature request, please DM me on Discord about it (@7gtz).