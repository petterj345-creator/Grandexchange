# Grandexchange

A RuneScape-style **Grand Exchange** marketplace plugin for **Minecraft Paper 1.21.11** (Java 21).

Players list items for sale, browse what others are selling, and buy any quantity —
typing the amount in chat. Listed items are escrowed (held by the plugin) until sold
or cancelled, so nothing is duplicated.

## Requirements

- A **Paper** server, Minecraft **1.21.11**
- **Vault** + any economy plugin (e.g. EssentialsX) for player balances
- SQLite is downloaded automatically at runtime via Paper's library loader
- **Citizens** (optional) — to open the exchange by right-clicking an NPC

## NPC clerk (Citizens)

If Citizens is installed, you can make any NPC open the Grand Exchange when right-clicked:

1. Select the NPC: `/npc select` (look at it, or use `/npc select <id>`)
2. Add the trait: `/trait grandexchange`

Right-clicking that NPC now opens the browse-and-buy GUI. The trait is saved with the
NPC, so it survives restarts. Remove it with `/trait remove grandexchange`.

## Commands

| Command | Description |
|---------|-------------|
| `/ge` (or `/grandexchange`, `/gx`) | Browse all listings and buy |
| `/ge sell` | Open the sell menu for the item in your hand |
| `/ge mine` | View and cancel your own listings (items are returned) |
| `/ge help` | Show command help |

## How it works

Everything is done from the GUI. Open it with `/ge` or by right-clicking a Grand
Exchange NPC. The bottom row has three tabs — **Browse**, **Sell an item**, and
**My listings** — so you never need a command.

### Selling
1. Open the GUI and click the **Sell an item** tab.
2. Click any item in **your own inventory** (shown below the menu) to choose it.
3. In the sell menu, set the **quantity** with the `+/-` buttons, "Sell all", or
   "Type amount in chat".
4. Set your **price per item** (type in chat), or click **Use market price** to match
   the current average.
5. The menu shows the **market price** — the average and lowest price other players have
   currently listed that same item for.
6. Click **Confirm**. The items are taken from your inventory and listed.

### Buying
1. Open the GUI (Browse tab) and click an item.
2. Type **how many** you want in chat.
3. You pay from your Vault balance; the seller is paid (minus the optional sale fee) and
   you receive the items.

### Managing your listings
Click the **My listings** tab, then click any of your listings to cancel it and get the
remaining items back.

### Config (`plugins/Grandexchange/config.yml`)

```yaml
tax-percent: 0.0            # % fee taken from each sale (seller receives price minus this)
max-listings-per-player: 20 # max simultaneous active listings per player
```

## Building

The jar is built automatically by GitHub Actions on every push to `main` and on pull
requests — download `Grandexchange-<version>.jar` from the **Actions** tab → latest
**Build** run → **Artifacts**.

### Building locally

No system Maven needed — use the bundled wrapper (requires JDK 21+):

```bash
./mvnw package      # Linux/macOS
mvnw.cmd package    # Windows
```

The jar is written to `target/Grandexchange-<version>.jar`.

## Installing

1. Install Vault + an economy plugin on your Paper server.
2. Drop `Grandexchange-<version>.jar` into `plugins/` and restart.

## Project layout

```
pom.xml                                  Maven build (Paper API, Vault API, Java 21)
src/main/resources/plugin.yml            Plugin metadata, command, sqlite library
src/main/resources/config.yml            Default config
src/main/java/.../Grandexchange.java     Plugin bootstrap
                  economy/EconomyHook    Vault wrapper
                  storage/               SQLite database + Listing/MarketStats models
                  input/                 Chat-prompt + sell-session state
                  gui/                   Browse menu + sell menu
                  listener/              Inventory clicks + chat capture
                  command/               /ge command
                  util/Items             Inventory count/remove/give helpers
.github/workflows/build.yml              GitHub Actions build
```
