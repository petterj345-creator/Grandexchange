# Grandexchange

A Minecraft **Paper** plugin targeting **Minecraft 1.21.11** (Java 21).

## Building

The jar is built automatically by GitHub Actions on every push to `main` and on
pull requests. Download the built `Grandexchange-<version>.jar` from the
**Actions** tab → latest **Build** run → **Artifacts**.

### Building locally (optional)

Requires JDK 21 and Maven:

```bash
mvn package
```

The jar is written to `target/Grandexchange-<version>.jar`.

## Installing

Drop the jar into your Paper server's `plugins/` folder and restart the server.

## Usage

| Command | Alias | Description |
|---------|-------|-------------|
| `/grandexchange` | `/ge` | Prints the running plugin version. |

## Project layout

```
pom.xml                                   Maven build config (Paper API, Java 21)
src/main/java/.../Grandexchange.java      Plugin main class
src/main/resources/plugin.yml             Plugin metadata + command registration
.github/workflows/build.yml               GitHub Actions build
```
