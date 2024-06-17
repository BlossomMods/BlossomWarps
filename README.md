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

## Commands & their permissions

- `/warp <warp>` - teleport to a warp point called `<warp>`  
  Permission: `blossom.warps.command.warp.base` (default: true)

  - `/warp <warp> <who>` - teleport a player to a warp point called `<warp>`  
    Permission: `blossom.warps.command.warp.others` (default: OP level 2)

- `/warps` - alias of `/warps list`  
  Permission: `blossom.warps.command.warps.base` (default: true)
  - `list` - list all available warps  
    Permission: `blossom.warps.command.warps.list` (default: true)

  - `add <name> [<position> <rotation>] [<dimension>]` - add a warp called `<warp>` in `<dimension>` at `<position>`
    with rotation: `<rotation>`. If position/rotation or dimension not provided, executors position will be used.  
    Permission: `blossom.warps.command.warps.add` (default: OP level 2)

  - `remove <name>` - removes a warp called `<name>`  
    Permission: `blossom.warps.command.warps.remove` (default: OP level 2)

  - `add-global <name>` - add warp as global warp (accessible with `/<name>` after server restart, behavior not defined
    if command `/<name>` already exists)  
    Permission: `blossom.warps.command.warps.add-global` (default: OP level 2)

Each warp has `blossom.warps.warp.<name>` (default: true)  
Each global warp has `blossom.warps.global-warp.<name>` (default: true)

## Translations

Since BlossomMods v3 translations have been moved to the BlossomLib mod.

`zh_cn` (Chinese, Simplified), `zh_tw` (Chinese, Traditional) - added by @BackWheel
