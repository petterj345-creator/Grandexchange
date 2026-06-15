# Grandexchange

A RuneScape-style **Grand Exchange** for **Minecraft Paper 1.21.11** (Java 21).

It's a real order book: players place **buy offers** and **sell offers** that match
automatically. Money and items are escrowed up front (coins when you place a buy, items
when you place a sell), trades execute at the **seller's price** — a buyer who offered
more is refunded the difference — and filled items/coins wait in a **collection box**.
Offers fill partially and the remainder keeps resting, just like the real GE.

## Requirements

- A **Paper** server, Minecraft **1.21.11**
- **Vault** + any economy plugin (e.g. EssentialsX) for player balances
- SQLite is downloaded automatically at runtime via Paper's library loader
- **Citizens** (optional) — to open the exchange by right-clicking an NPC

## NPC clerk (Citizens)

If Citizens is installed, you can make any NPC open the Grand Exchange when right-clicked:

1. Select the NPC: `/npc select` (look at it, or use `/npc select <id>`)
2. Add the trait: `/trait grandexchange`

Right-clicking that NPC now opens the exchange GUI. The trait is saved with the
NPC, so it survives restarts. Remove it with `/trait remove grandexchange`.

## Commands

Everything is reachable from the GUI tabs — the commands are just shortcuts.

| Command | Description |
|---------|-------------|
| `/ge` (or `/grandexchange`, `/gx`) | Open the market / price list |
| `/ge sell` | Start selling the item in your hand |
| `/ge offers` | View and cancel your buy/sell offers |
| `/ge collect` | Open your collection box |

## How it works

Buy orders and sell orders live in **separate windows**. The GUI has four tabs:
**Buy** (sell orders — things for sale), **Sell** (buy orders — things wanted),
**My offers**, and **Collection box**.

### Buy window (`Buy` tab)
Lists every resting **sell order** as its own row — several orders for the same item each
appear separately, cheapest first — showing each order's price, quantity and seller.

To buy, either **click a sell order** — this opens a one-click confirmation locked to that
order's price and quantity, so you just press **Accept** to buy it — or click **Create a
buy offer** and pick an item from your own inventory (a sample) to compose your own offer;
this works even when nobody is selling it yet. In the builder you adjust the **quantity**
and your **max price each** → **Confirm**. `quantity × max price` is reserved from your
balance, then:

- It **instantly fills** against any sellers at your price **or lower** — you pay *their*
  price (refunded the difference) and the items land in your **collection box**.
- Anything unfilled keeps **resting**, and fills automatically as new sellers appear
  (those later items also go to your **collection box**, since you may be offline).

### Sell window (`Sell` tab)
Lists every resting **buy order** as its own row — several orders for the same item each
appear separately, highest-paying first — showing each order's price, quantity and buyer.

- **Click a buy order** to sell into it — this opens the sell builder pre-filled to that
  order's price and quantity (capped by what you hold), so you can sell **any amount you
  have** (e.g. 100 into a buy order that wants 1000) and then **Confirm**. The **Sell exact
  amount wanted** button snaps your quantity to total demand at your price.
- Or click **Sell from inventory** to compose a sell offer at your own price.

Selling escrows your items; matched coins land in your **collection box**, and any
unfilled remainder rests and stays buyable.

### Collection box
Items and coins from every fill — whether it happened instantly or while your offer was
resting — wait in the **Collection** tab. Click an entry to take it out, or **Collect
everything**.
Cancelling an offer from **My offers** returns its remainder (items, or reserved coins)
to you immediately.

### Config (`plugins/Grandexchange/config.yml`)

```yaml
max-listings-per-player: 20 # max simultaneous active offers per player
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
                  storage/               SQLite DB + Offer/CollectionEntry/MarketSummary
                  engine/                MatchingEngine (escrow, matching, collection box)
                  input/                 Chat prompts + buy/sell session state
                  gui/                   Market, item detail, buy/sell, my offers, collection
                  service/ExchangeService  Opens screens + runs place/cancel/collect
                  listener/              Inventory clicks + chat capture
                  citizens/              Optional Citizens NPC trait + listener
                  command/               /ge command
                  util/                  Items + Gui helpers
.github/workflows/build.yml              GitHub Actions build
```
