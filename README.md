# BlossomWarps

BlossomWarps is a Minecraft Fabric mod in the Blossom-series mods that provides /warp command and utilities

## Table of contents

- [Dependencies](#dependencies)
- [Config](#config)
- [Commands & their permissions](#commands--their-permissions)
- [Translation keys](#translation-keys)

## Dependencies

* [BlossomLib](https://github.com/BlossomMods/BlossomLib)
* [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) / [LuckPerms](https://luckperms.net/) /
  etc. (Optional)

## Config

This library's config file can be found at `config/BlossomMods/BlossomWarps.json`, after running the server with
the mod at least once.

`teleportation`: [TeleportationConfig](https://github.com/BlossomMods/BlossomLib/blob/main/README.md#teleportationconfig) - 
  teleportation settings  
`standStill`: int - (seconds), how long the player has to stand still before being teleported  
`cooldown`: int - (seconds), how long the player has to wait after teleporting using this command, before
  being able to teleport again

## Commands & their permissions

- `/warp <warp>` - alias of `/warp <warp> {self}`  
  Permission: `blossom.warps.warp` (default: true)
- `/warp <warp> <who>` - teleport a player to a warp point called `<warp>`  
  Permission: `blossom.warps.warp.others` (default: OP level 2)
- `/warps` - alias of `/warps list`  
  Permission: `blossom.warps.warps` (default: true)
  - `list` - list all available warps  
  - `add <name> [<position> <rotation>] [<dimension>]` - add a warp called `<warp>` in `<dimension>` at `<position>` with rotation: `<rotation>`.
    If position/rotation or dimension not provided, executors position will be used.  
    Permission: `blossom.warps.add` (default: OP level 2)
  - `remove <name>` - removes a warp called `<name>`  
    Permission: `blossom.warps.remove` (default: OP level 2)

## Translation keys
only keys with available arguments are shown, for full list, please see
[`src/main/resources/data/blossom/lang/en_us.json`](src/main/resources/data/blossom/lang/en_us.json)

- `blossom.warps.add`: 1 argument - warp name
- `blossom.warps.add.failed`: 1 argument - warp name
- `blossom.warps.remove`: 1 argument - warp name
- `blossom.warps.remove.failed`: 1 argument - warp name
- `blossom.warps.list.dimension.header`: 1 argument - dimension key
- `blossom.warps.list.item`: 1 argument - warp name
- `blossom.warps.list.item.description`: 8 arguments - warp name, warp owner UUID, warp dimension key, warp x, warp y, warp z, warp yaw, warp pitch
