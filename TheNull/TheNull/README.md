# TheNull

A Paper 1.21.11 plugin that adds **The Null**, a mysterious faction inspired by the Null from Unstable SMP.

* Java 21, Gradle, package `com.thegildedking.thenull`
* Nobody ever becomes Null automatically: only an admin command or an accepted Totem invitation does it.

## Features

* **Random identities** (A-Z, 0-9, 5-8 characters by default, unique, saved forever).
* **Four commanders**: The Sovereign, The Executioner, The Harbinger, The Watcher. One player per position, admin-assigned only. They keep their random identity and get a special nametag format.
* **Totem of Null**: a real Totem of Undying (Curse of Vanishing, PersistentDataContainer tag). Right-click a player with it to open the YES / NO invitation menu on *their* screen. When it pops, **Null Erasure** kills every online Null (commanders included).
* **Invitation rules**: only the target can answer, 30 s expiry (configurable), cancelled when either player disconnects, one invitation per target, Nulls cannot be invited, the totem is not consumed, GUI clicks/drags/shift-clicks are all cancelled.
* **Appearance**: real skin swap via Paper's `Player#setPlayerProfile` (no extra plugin needed), identity nametag, tab list name, chat name, join/quit/death messages.
* **Persistence**: `plugins/TheNull/data.yml`, written atomically on every change (with a `.bak` copy). Survives restarts, reconnects and reloads.
* **Offline removal**: `/null remove` works on offline players; they are cleaned up on their next join.

## Black skin setup (one time, 2 minutes)

Minecraft only accepts skins hosted by Mojang, so a plugin cannot invent a black skin from nothing. The plugin needs the *texture value + signature* of a black skin. Use ONE of these:

**Option A: your own account (easiest)**
1. Take `skin/null_black_skin.png` from this project.
2. Go to <https://www.minecraft.net/en-us/msaprofile/mygames/editskin>, upload it and save (classic model).
3. Join your server, make sure you have `thenull.admin`, and run **`/null setskin`**. This copies your current signed skin into `plugins/TheNull/skin.yml`.
4. Change your own skin back afterwards. New Nulls will use the black skin from now on.

**Option B: MineSkin website**
1. Upload `skin/null_black_skin.png` at <https://mineskin.org>.
2. Copy the `value` and `signature` it gives you into `skin.texture-value` and `skin.texture-signature` in `config.yml`, then `/null reload`.

Until this is done the plugin still works, but Nulls only get the identity nametag and keep their normal skin. It tells you this on startup and when you transform someone.

## Commands and permissions

| Command | Permission |
|---|---|
| `/null transform <player> [commander]` | `thenull.transform` (+ `thenull.commander` for a position) |
| `/null remove <player>` | `thenull.transform` |
| `/null commander <player> <position>` | `thenull.commander` |
| `/null commander remove <position>` | `thenull.commander` |
| `/null give <player>` | `thenull.totem` |
| `/null info <player>` | `thenull.transform` |
| `/null list` | `thenull.transform` |
| `/null setskin` | `thenull.admin` |
| `/null reload` | `thenull.admin` |

`thenull.admin` includes all the others. Targets can be a username (online, offline, or a Null's real name) or a Null identity.
Positions: `sovereign`, `executioner`, `harbinger`, `watcher`.

## Building

Push to GitHub. The workflow in `.github/workflows/build.yml` (you add it yourself) builds with Gradle 8.10.2 on Java 21 and uploads `TheNull-1.0.0.jar` as an artifact. Locally: `gradle build` (Java 21 + Gradle 8.x) and take the jar from `build/libs/`.

## Installation

Copy the jar into `plugins/` of a **Paper 1.21.11** server. No other plugins or libraries are required.

## Known limitations

* The vanilla nametag is hidden with a scoreboard team (`thenull_hidden`) and replaced by a floating text label. A player can only be in one scoreboard team, so another plugin that assigns teams to the same player may conflict.
* The label is a client-side-rendered entity, so it is not identical to a vanilla nametag (tune `nametag.label-height` if it overlaps the head).
* A Null's own first-person/F5 view of their skin may only update after they reconnect; other players see the change immediately.
* Real usernames can still leak through other plugins, tab-completion of `/msg`, advancement messages, etc. Only the things listed in "Appearance" are replaced.
