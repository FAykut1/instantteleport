# InstantTeleport

InstantTeleport is a beta NeoForge Minecraft mod that adds a rechargeable teleportation device with saved destination slots and hotkey teleporting.

Current release line: `1.0.0-beta.1` for Minecraft `26.1.2` and NeoForge `26.1.2.63-beta`.

## Features

- `Teleportation Device` with 9 saved destination slots.
- `Creative Teleportation Device` for testing, with infinite energy.
- Compact right-click GUI for saving, renaming, and removing destinations.
- `Alt + 1` through `Alt + 9` keybinds to teleport to saved slots.
- JEI is included only in the local Gradle runtime for development/testing.

## Usage

- Hold the device and `Right Click` to open the destination editor.
- `Shift + Right Click` saves the current position into the first empty slot.
- Use `Set` in the GUI to save the current position to a specific slot.
- Use `Name` to rename a saved slot.
- Use `X` to remove a saved slot.
- Press `Alt + 1` through `Alt + 9` to teleport to the matching saved slot.

## Recipe

The normal device is intended as a late-game item:

```text
D E D
N S N
D R D
```

- `D` = Diamond
- `E` = End Crystal
- `N` = Netherite Ingot
- `S` = Nether Star
- `R` = Recovery Compass

The creative device is available from the mod creative tab for testing.

## Development

Build the mod:

```bash
./gradlew build
```

Run the development client:

```bash
./gradlew runClient
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

## Dependencies

- Required: NeoForge
- Optional: accessory slot integration
- Development runtime only: JEI

The mod works normally with the device in the player's hands.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
