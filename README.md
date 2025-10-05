# WarpSpeed

A feature-rich Minecraft Fabric mod that provides advanced teleportation capabilities including homes, warps, spawn teleportation, and a `/back` command to return to previous locations.

## ✨ Features

### 🏠 Home System
- Set multiple home locations (up to 10 per player)
- Teleport to any of your saved homes
- Easy home management with list, set, and delete commands
- Tab completion for home names

### 🌟 Warp System
- Create public or private warp points (up to 254 per player)
- Public warps are accessible to all players
- Private warps are only accessible to their creator
- Tab completion for warp names
- Ownership validation for warp deletion

### 🔙 Back Command
- Return to your previous location after teleporting
- Works with spawn, home, and warp teleporting
- Returns to death location after dying
- Supports "ping-pong" teleportation between two locations

### 🏔️ Spawn Teleportation
- Quick teleportation to Overworld spawn

### 🎨 Visual & Audio Effects
- Beautiful particle effects on departure and arrival
- Portal particles with enchantment sparkles
- Immersive sound effects

### 🛡️ Safety Features
- Safe location finding with 3-block search radius
- Cross-dimension validation and support
- Input validation for home/warp names (alphanumeric, 1-32 characters)
- Automatic cleanup of player data on disconnect
- Protection against invalid world references

## 📋 Commands

### Home Commands
- `/home <name>` - Teleport to a saved home
- `/setHome <name>` - Set a home at your current location
- `/delHome <name>` - Delete a saved home
- `/homes` - List all your saved homes

### Warp Commands
- `/warp <name>` - Teleport to a warp point
- `/setWarp <name> <true|false>` - Create a warp (true = private, false = public)
- `/delWarp <name>` - Delete one of your warp points

### Other Commands
- `/spawn` - Teleport to Overworld spawn (works from any dimension)
- `/back` - Return to your previous location or death point

## 🎮 Tab Completion

All commands support intelligent tab completion:
- `/home` - Shows your homes
- `/delHome` - Shows your homes
- `/warp` - Shows all accessible warps (public + your private)
- `/delWarp` - Shows only your warps
- `/setWarp <name>` - Suggests `true` or `false` for privacy setting

## 📦 Installation

1. Ensure you have [Fabric Loader](https://fabricmc.net/use/) installed
2. Download the latest WarpSpeed mod jar from [Releases](../../releases)
3. Place the jar file in your `.minecraft/mods` folder
4. Launch Minecraft with the Fabric profile

## ⚙️ Requirements

- Minecraft 1.21+ (Fabric)
- Java 21+
- Fabric API (dependency)

## 💾 Data Storage

WarpSpeed uses SQLite to store all home and warp data persistently. The database is automatically created at:
`config/warpspeed/warp_points.db`

Data persists across server restarts and includes:
- Player homes with coordinates and world information
- Warp points with privacy settings and ownership
- Automatic indexing for fast lookups
- Timestamps for all entries

## 🔧 Technical Features

### For Server Administrators
- No external dependencies beyond Fabric API
- Lightweight SQLite database with automatic creation
- Configurable limits (currently 10 homes, 254 warps per player)
- Comprehensive logging for debugging
- SQL injection protection via prepared statements
- Cross-dimension teleportation support
- Memory-efficient player location tracking

### For Developers
- Clean separation of concerns (commands, utils, mixins)
- Proper exception handling throughout
- DRY principles applied (centralized teleportation logic)
- Mixin integration for death tracking
- Tab completion support for all commands
- Particle effect system for visual feedback
- Cross-dimension teleportation with TeleportTarget API

## 🌍 Cross-Dimension Support

WarpSpeed fully supports cross-dimension teleportation:
- Teleport from The End to Overworld homes
- Teleport from Nether to Overworld spawn
- Use `/back` to return across dimensions
- Warps work across all dimensions
- Automatic dimension detection and handling

## 🎯 Known Limitations

- Maximum of 10 homes per player (configurable in the future)
- Maximum of 254 warps per player (configurable in the future)
- Warp names must be globally unique (not per-player)
- Safe location search has a 3-block radius

## 🔮 Future Considerations

Potential features for future releases:
- Configuration file for limits, cooldowns, and particle effects
- Teleport cooldowns to prevent spam
- Warp descriptions and categories
- Warp popularity tracking
- Permission system for server administrators

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## 📄 License

This project is licensed under the Mozilla Public License 2.0 (MPL-2.0).

See [LICENSE.txt](LICENSE.txt) for details.

### What this means:
- ✅ You can use this mod freely in modpacks
- ✅ You can modify the code (but must share modifications)
- ✅ You can use it commercially
- ✅ Patent rights are granted
- ❌ Modified files must remain MPL 2.0 licensed

For more information, see the [MPL 2.0 FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).

## 👤 Credits

Developed by finleyofthewoods

Built with:
- [Fabric](https://fabricmc.net/) - Mod loader
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) - Database connectivity
- [SLF4J](https://www.slf4j.org/) - Logging framework

## 🐛 Support

For issues, questions, or suggestions:
- Open an [Issue](../../issues) on GitHub
- Check existing issues for solutions
- 
---

**Note**: This mod is designed for Fabric. It will not work with Forge or other mod loaders.

---

**Enjoy your enhanced teleportation experience!** ✨🚀