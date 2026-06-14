# Grandexchange

A RuneScape-style **Grand Exchange** marketplace plugin for **Minecraft Paper 1.21.11** (Java 21).

Players list items for sale, browse what others are selling, and buy any quantity —
typing the amount in chat. Listed items are escrowed (held by the plugin) until sold
or cancelled, so nothing is duplicated.

## Requirements

- A **Paper** server, Minecraft **1.21.11**
- **Vault** + any economy plugin (e.g. EssentialsX) for player balances
- SQLite is downloaded automatically at runtime via Paper's library loader

## Commands

| Command | Description |
|---------|-------------|
| `/ge` (or `/grandexchange`, `/gx`) | Browse all listings and buy |
| `/ge sell` | Open the sell menu for the item in your hand |
| `/ge mine` | View and cancel your own listings (items are returned) |
| `/ge help` | Show command help |

## How it works

### Selling
1. Hold the item you want to sell and run `/ge sell`.
2. In the sell menu, set the **quantity** with the `+/-` buttons, "Sell all", or
   "Type amount in chat".
3. Set your **price per item** (type in chat), or click **Use market price** to match
   the current average.
4. The menu shows the **market price** — the average and lowest price other players have
   currently listed that same item for.
5. Click **Confirm**. The items are taken from your inventory and listed.

### Buying
1. Run `/ge` and click an item.
2. Type **how many** you want in chat.
3. You pay from your Vault balance; the seller is paid (minus the optional sale fee) and
   you receive the items.

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
