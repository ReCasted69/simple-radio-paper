# 📻 SimpleRadio

A wireless radio system for Paper 1.21.x. Build real radios in-world, tune them to a frequency, and beam redstone signals — or your voice — across the map.

## ✨ Features

- 🔧 **Buildable radios** — drop a chiseled copper base and stack a lightning-rod antenna. That's it, the radio assembles itself.
- 📡 **Three modes, one radio** — switch between Transmitter, Redstone Receiver, and Sound Receiver with a single click.
- 🎛️ **Simple tuning GUI** — right-click the base to open the radio and dial in any frequency from 0.1 to 108.0 MHz.
- 🔴 **Wireless redstone** — carry signals over the air with no wires, repeaters, or redstone dust between points.
- 🎙️ **Voice relay** *(optional)* — with [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) installed, your radio transmits nearby voice too.
- 🌍 **Real link-budget physics** — distance, terrain, and obstacles all weaken your signal.
- 📶 **Signal quality & interference** — competing transmitters on the same frequency cause static and flicker.
- ⚡ **Lightning overload** — a direct strike can fry an antenna and burn out the base.
- 📖 **Fully configurable** — every setting is documented in `config.yml`, and every message is editable in `messages.yml`.

## 🚀 Quick start

1. Drop the jar into your server's `plugins/` folder.
2. (Optional) Add [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) for voice relay.
3. Start the server and start building radios!

## 📻 Building your first radio

1. Place a **chiseled copper block** (waxed or unwaxed) as the base.
2. Build an antenna straight up from it using **lightning rods**, **copper blocks**, and/or **netherite blocks** — at least one lightning rod is required. The radio is detected automatically.
3. Right-click the base with an **empty hand or a tool** to open the GUI (sneak + right-click if you're holding a block).
4. Pick a frequency and a mode:
   - 🔼 **Transmitter** — power it with redstone to broadcast.
   - 🔽 **Redstone Receiver** — the base becomes a redstone output while it receives.
   - 🔊 **Sound Receiver** — plays received voice through Simple Voice Chat.
5. Optionally place a **note block** next to the base to use it as the speaker position.

## ⌨️ Commands

| Command | Description |
|---|---|
| `/radio help` | Show build and usage help |
| `/radio setfreq <mhz>` | Set the open radio's frequency |
| `/radio mode <transmitter\|redstone\|sound>` | Set the open radio's mode |
| `/radio scan` | Toggle frequency scanning |
| `/radio info` | Show the open radio's status |
| `/radio list` | List registered radios *(admin)* |
| `/radio debug [x y z]` | Print radio diagnostics *(admin)* |
| `/radio reload` | Reload config and messages *(admin)* |

## 🔑 Permissions

| Permission | Default | Description |
|---|---|---|
| `simpleradio.use` | `true` | Use and configure radios |
| `simpleradio.assemble` | `true` | Build radios |
| `simpleradio.admin` | `op` | Admin commands |

## ⚙️ Configuration

All physics, gameplay, voice, and lightning settings live in `config.yml` with inline documentation. Player-facing text lives in `messages.yml`.

Voice relay works out of the box without redstone keying — set `voice.require-keyed: true` if you want transmitters to relay voice only while powered.
