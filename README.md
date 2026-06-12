<div align="center">

# WaffleHomes

Modern homes, RTP and teleport requests for Paper servers.

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-3C8527?style=for-the-badge&logo=minecraft&logoColor=white)
![Paper](https://img.shields.io/badge/Paper-26.1.2-FFFFFF?style=for-the-badge&logo=papermc&logoColor=000000)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-Database-003B57?style=for-the-badge&logo=sqlite&logoColor=white)
![Geyser](https://img.shields.io/badge/Geyser-Compatible-00AEEF?style=for-the-badge)
![Floodgate](https://img.shields.io/badge/Floodgate-Compatible-00AEEF?style=for-the-badge)
![License](https://img.shields.io/badge/License-All_Rights_Reserved-red?style=for-the-badge)

</div>

---

## Overview

WaffleHomes is a lightweight Paper plugin focused on homes, teleport requests and safe random teleports while maintaining full compatibility with Java and Bedrock players through Geyser and Floodgate.

Designed for Paper 26.1.2 servers, the plugin provides modern interfaces, SQLite storage and reliable cross-platform support without requiring additional dependencies.

---

## Features

### Homes

- Create multiple homes
- Instant home teleportation
- Home management menu
- SQLite storage
- Fast and lightweight

### Random Teleport

- Safe surface detection
- Avoids dangerous blocks
- Configurable radius
- Bedrock compatible

### Teleport Requests

- `/tpa`
- `/tphere`
- `/tpaccept`
- `/tpdeny`
- Request timeout system

### Bedrock Support

- Floodgate Forms
- Mobile friendly
- Controller friendly
- Console friendly
- Touchscreen optimized

### Administration

- View player homes
- Delete player homes
- Rename player homes
- Teleport to player homes

---

## Compatibility

| Platform | Support |
|----------|----------|
| Paper 26.1.2 | ✔ |
| Geyser | ✔ |
| Floodgate | ✔ |
| Java Edition | ✔ |
| Bedrock Edition | ✔ |
| Android | ✔ |
| iOS | ✔ |
| Windows Bedrock | ✔ |
| Xbox | ✔ |
| PlayStation | ✔ |
| Nintendo Switch | ✔ |

---

## Commands

| Command | Description |
|----------|-------------|
| `/sethome <name>` | Create a home |
| `/home <name>` | Teleport to a home |
| `/homes` | Open homes menu |
| `/delhome <name>` | Delete a home |
| `/rtp` | Random teleport |
| `/tpa <player>` | Send teleport request |
| `/tphere <player>` | Request teleport here |
| `/tpaccept` | Accept request |
| `/tpdeny` | Deny request |
| `/tpsettings` | Teleport settings |
| `/adminhomes <player>` | View homes |
| `/adminedithomes <player>` | Edit homes |
| `/admindelhomes <player>` | Delete homes |

---

## Permissions

| Permission | Description |
|------------|-------------|
| `wafflehomes.home` | Basic home commands |
| `wafflehomes.rtp` | Random teleport |
| `wafflehomes.tpa` | Teleport requests |
| `wafflehomes.admin` | Administrative commands |

---

## Installation

1. Download the latest release.
2. Place the jar file inside the `plugins` folder.
3. Start or restart the server.
4. Configure the plugin if needed.
5. Enjoy.

---

## Storage

WaffleHomes uses SQLite for persistent storage.

Stored data:

- Homes
- Home settings
- Teleport preferences
- Pending requests

No external database is required.

---

## Built With

![Java](https://img.shields.io/badge/OpenJDK-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Paper](https://img.shields.io/badge/Paper_API-26.1.2-white?style=flat-square&logo=papermc&logoColor=black)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=flat-square&logo=sqlite&logoColor=white)

---

## License

All Rights Reserved.

This software may be used on personal or public servers.

Redistribution, resale, republishing, or distribution of modified versions is not permitted without explicit permission from the author.

---

## Credits

Developed by WafflyCatt.

Built for modern crossplay Paper servers.