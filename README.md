# MCBackpack Core Extension (PaperMC, SpigotMC & FoliaMC)

The Core extension for MCBackpack provides the out-of-the-box backpack experience: a `/bp` command, inventory listeners, password protection, and item customization. It is built as a multi-module, multi-platform project that runs on PaperMC, SpigotMC, and FoliaMC servers.

## Features

- **Dynamic Command System**: Registers the `/bp` (alias `/backpack`) command for players and console without requiring a `plugin.yml` entry.
- **Backpack Creation**: Generates customizable backpack items with a configurable size (multiples of 9, from 9 to 54) and texture.
- **Inventory Persistence**: Saves and restores backpack contents through the MCBackpack provider API.
- **Password Protection**: Locks backpacks with PBKDF2/SHA-256 hashed passwords, with secure chat-based password entry.
- **Texture & Model Data Applicators**: Special items that apply a new head texture or custom model data to an existing backpack.
- **Recursive Storage Prevention**: Stops players from storing a backpack inside another backpack.
- **Reopen Cooldown**: Prevents rapid open/close spam while data is being persisted.

## Commands

- `/bp create [size] [texture] [model_data]`: Get a new backpack creation item.
- `/bp texture <base64_texture>`: Get a backpack texture applicator.
- `/bp get model data <model_data>`: Get a model data applicator.
- `/bp setpwd <password>`: Set a password on the backpack in your main hand.
- `/bp changepwd <old> <new>`: Change the password on the held backpack.
- `/bp deletepwd <password>`: Remove the password from the held backpack.
- `/bp help`: Display the help menu.

## Project Structure

This project follows a multi-module, multi-platform layout:

- `bukkit`: Shared, platform-independent backpack domain logic (commands, listeners, managers, tab completer).
- `platforms/papermc`: PaperMC entry point (`Core`).
- `platforms/spigotmc`: SpigotMC entry point (`Core`).
- `platforms/foliamc`: FoliaMC entry point (`Core`).

## Installation

1. Ensure the [MCBackpack Plugin](https://github.com/MCValac) is installed on your PaperMC, SpigotMC, or FoliaMC server.
2. Place the compiled `Core-*.jar` into the following directory:

   ```text
   plugins/MCBackpack/extensions/libs
   ```
3. Restart the server or reload the extension manager.

## Requirements

- **Java**: 25+
- **MCBackpack Plugin**: Latest version
- **MCExtension**: 2026.0.6-5

## Build Instructions

To build the extension from source:
```bash
./gradlew build
```
The compiled artifact will be located in `build/libs/`.
