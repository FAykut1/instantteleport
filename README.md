# InstantTeleport

InstantTeleport is a NeoForge Minecraft mod that adds a rechargeable teleportation device with saved destination slots, hotkey teleporting, and optional Curios support.

This branch targets Minecraft `1.21.1` with NeoForge `21.1.228`.

## Features

- `Teleportation Device` with 9 saved destination slots.
- `Creative Teleportation Device` for testing, with infinite energy.
- Compact right-click GUI for saving, renaming, and removing destinations.
- `Alt + 1` through `Alt + 9` keybinds to teleport to saved slots.
- Optional Curios integration with a dedicated `Teleport Device` slot.
- JEI is included only in the local Gradle runtime for development/testing.

## Usage

- Hold the device and `Right Click` to open the destination editor.
- `Shift + Right Click` saves the current position into the first empty slot.
- Use `Set` in the GUI to save the current position to a specific slot.
- Use `Name` to rename a saved slot.
- Use `X` to remove a saved slot.
- Press `Alt + 1` through `Alt + 9` to teleport to the matching saved slot.

If Curios is installed, the device can be equipped in the Curios teleport slot and used with the keybinds without holding it.

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
- Optional: Curios API
- Development runtime only: JEI

Curios is optional at runtime. The mod still works without Curios by using the device from the player's hands.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
