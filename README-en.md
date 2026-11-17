[中文](README.md) | [English](README-en.md)

### This project is fully handled by Deepseek-v4-pro and Qwen3

# JSON Registered

For Modpack Makers.
A Fabric mod that enables rapid registration of items, blocks, and fluids via JSON configuration files.  
No coding required – just edit the config files to add custom content to the game.

## ✨ Features

- 🎯 **Zero-code development**: Only need to modify JSON configuration files
- 🔄 **Runtime resource generation**: After much AI tuning, integrates a runtime resource pack – like KubeJS, no tedious copying of default models
- 📦 **Full support**: Items, blocks, and fluids are all supported!

## Notes before the AI-generated README
- Dependency is **YARRP Mod**
- Fluid bucket textures are **generated automatically**: the vanilla iron bucket sprite is used as the base, and a slice of the `<id>_still` fluid texture is composited into the bucket opening. No hand-drawn bucket texture is needed; use `bucket_texture` if you want a wooden/custom bucket body.
- The bucket item is **always registered** when the fluid has a block (`has_block`), because the "fill bucket" chain depends on it: without a bucket item, vanilla buckets and TechReborn fluid cells delete the fluid block but return nothing.

### Important Notes
1. **Restart required**: Changes to configuration files take effect only after restarting the game
2. **First-run generation**: On first launch, example configuration files are automatically generated
3. **Error handling**: If JSON format is invalid, the mod logs errors and continues with an empty configuration – no game crash
4. **ID conflicts**: Blocks and their corresponding block items use the same ID; you cannot have a standalone item with the same ID at the same time
5. **Fluid properties**: `viscosity` affects flow speed (higher = slower, 1000 ≈ water); `density` below 1000 makes the fluid flow upward (light fluid / gas effect); `temperature` is not yet functional; **fluid fields left unset automatically fall back to vanilla water logic** (viscosity 1000, density 1000 flowing downward, temperature 300K)
6. **Resource priority**: Auto-generated resources can be overridden by traditional resource packs
7. **Fluid block textures**: Fluid blocks require texture files named `<id>_still.png` and `<id>_flow.png`
8. **Gas flow**: a fluid with density < 1000 is the mirror image of vanilla falling water – it stays full-height while there is the same fluid below it, forming a rising column; once blocked it spreads sideways, losing one level per block (up to 7). Once settled, gas blocks no longer recompute themselves, so they stop producing fluid ticks.

### Texture Requirements
- **Format**: PNG files
- **Size**: Recommended 16×16 or 32×32 pixels
- **Naming**: Must match the `id` in the configuration file
- **Transparency**: For blocks with `render_layer` set to `cutoff`/`translucent`, use PNG textures with an alpha channel

## Installation

Ensure the following dependencies are installed:
- Minecraft 1.21.1
- Fabric Loader ≥ 0.19.2
- Fabric API ≥ 0.116.11
- Fabric Language Kotlin ≥ 1.13.11
- YARRP ≥ 0.2.0

## Configuration Structure

File location: `.minecraft/config/jsonreg_entries.json`

The root object contains three arrays:

```json
{
  "items": [],
  "blocks": [],
  "fluids": []
}
```

Each entry is a JSON object with fields described below.

### Item Configuration (`items`)

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `id` | String | Yes | - | Unique identifier for the item; registered as `jsonreg:<id>` |
| `name` | String | No | Same as `id` | Display name of the item (supports multilingual, e.g., Chinese) |
| `max_count` | Integer | No | 64 | Maximum stack size (1–64) |

Example:

```json
{
  "id": "copper_plate", 
  "name": "Copper Plate",
  "max_count": 64
}
```

### Block Configuration (`blocks`)

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `id` | String | Yes | - | Unique identifier for the block |
| `name` | String | No | Same as `id` | Display name of the block |
| `hardness` | Float | No | 1.5 | Block hardness (affects mining speed) |
| `resistance` | Float | No | 6.0 | Blast resistance |
| `requires_tool` | Boolean | No | false | Whether a tool is required to drop the item (vanilla semantics: without a suitable tool the block still breaks, but drops nothing) |
| `tool` | String | No | `none` | Which tool family can harvest this block: `pickaxe` / `axe` / `shovel` / `hoe` / `none`. Defaults to `pickaxe` when `requires_tool=true` and `tool` is omitted |
| `has_item` | Boolean | No | true | Whether to automatically create the corresponding block item (when true, the block drops itself) |
| `render_layer` | String | No | `solid` | Render layer: `solid` (default) / `cutoff` / `translucent`; the latter two behave like vanilla glass (light passes through, shared faces hidden between same blocks) |

`render_layer` options: `solid` (opaque, default), `cutoff` (cutout transparency, e.g., leaves), `translucent` (semi-transparent, e.g., glass). Blocks with `cutoff`/`translucent` behave like vanilla glass: light passes through completely, shared faces are hidden when adjacent to the same block, and faces facing them are drawn normally by adjacent solid blocks.

Blocks with `has_item=true` automatically get a "drops itself" loot table (same shape as vanilla `dirt.json`: `minecraft:block` + `survives_explosion`), so mining drops the block item while explosions destroy it without drops; with `requires_tool=true`, mining with the wrong tool drops nothing (vanilla behaviour). Blocks with `has_item=false` have no item, so no loot table is generated and they drop nothing.

> **What `tool` is for**: in 1.21 "which tool can harvest this block" is not decided by `requires_tool` but by whether the block is in a `minecraft:mineable/<tool>` tag (the tool component's rule table: in `#minecraft:mineable/pickaxe` → pickaxes can harvest; in `#minecraft:incorrect_for_iron_tool` → iron tools cannot). Custom blocks are in no mineable tag by default, so `requires_tool: true` alone would mean **no tool can ever harvest it**. This mod writes such blocks into the matching tag based on `tool`; when neither field is set the block stays "harvestable by hand and drops itself".

Example:

```json
{
  "id": "reinforced_glass",
  "name": "Reinforced Glass",
  "hardness": 2.5,
  "resistance": 10.0,
  "requires_tool": false,
  "has_item": true,
  "render_layer": "translucent"
}
```

### Fluid Configuration (`fluids`)

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `id` | String | Yes | - | Unique identifier for the fluid |
| `name` | String | No | Same as `id` | Display name of the fluid |
| `viscosity` | Integer | No | 1000 | Viscosity (affects flow speed; higher = slower, 1000 ≈ water) |
| `density` | Integer | No | 1000 | Density (<1000 flows upward, ≥1000 flows downward) |
| `luminosity` | Integer | No | 0 | Light level (0–15) |
| `temperature` | Integer | No | 300 | Temperature in Kelvin (not yet functional) |
| `has_block` | Boolean | No | true | Whether to create a fluid block |
| `bucket_texture` | String | No | `minecraft:item/bucket` | Base bucket texture used by the generated bucket model (16×16) |

Fluid fields left unset automatically fall back to vanilla water logic: viscosity 1000 (flow interval of 5 ticks), density 1000 (flowing downward), luminosity 0, temperature 300K, and the fluid block is created by default (`has_block` defaults to `true`).

> **About the bucket item**: whenever `has_block` is `true`, `jsonreg:<id>_bucket` is registered together with its model and lang entry.
> Reason: `FluidBlock` relies on `fluid.getBucketItem()` when draining; if that returns air, vanilla buckets and TechReborn fluid cells
> delete the fluid block and then receive an empty stack, i.e. "the fluid is consumed but no filled bucket is returned".
> The bucket model is composited dynamically from **the vanilla iron bucket plus a slice of the fluid texture**, so no `<id>_bucket.png` is needed.
> Conversely, a fluid with `has_block=false` gets no bucket – without a fluid block a bucket could neither fill nor place, it would just be wasted.

Example:

```json
{
  "id": "molten_copper",
  "name": "Molten Copper",
  "viscosity": 2000,
  "density": 3000,
  "luminosity": 10,
  "temperature": 1370,
  "has_block": true
}
```

## 🎨 Auto‑generated Resources

The mod automatically generates the following:

### Localisation Files
- `zh_cn` translation file
- Item format: `item.jsonreg.<id>` → `<name>`
- Block format: `block.jsonreg.<id>` → `<name>`
- Fluid bucket format: `item.jsonreg.<id>_bucket` → `<name> Bucket`

### Item Models
- Based on the `minecraft:item/generated` template
- Automatically sets `layer0` texture path: `jsonreg:item/<id>`

### Block Models
- Based on the `minecraft:block/cube_all` template (all six faces use the same texture)
- Automatically sets the `all` texture path: `jsonreg:block/<id>`
- Automatically creates the blockstate file
- Automatically creates the block item model

### Block Loot Tables
- Path: `data/jsonreg/loot_table/blocks/<id>.json` (injected via YARRP's runtime data pack)
- Equivalent to vanilla "drops itself": `minecraft:block` type, `survives_explosion` condition, drops `jsonreg:<id>`
- Only generated for blocks with `has_item=true`

### Mineable Tool Tags
- Path: `data/minecraft/tags/block/mineable/<tool>.json`
- Adds blocks to `minecraft:mineable/pickaxe|axe|shovel|hoe` according to the `tool` field
- Tags merge by default (`replace=false`), so vanilla contents are preserved

### Fluid Block Models
- Based on the `minecraft:block/cube_all` template
- Automatically sets the texture path: `jsonreg:block/<id>_still`
- Automatically creates the fluid blockstate file

### Fluid Bucket Models
- Generated as a flat item model (no parent template needed)
- Base layer: `bucket_texture`, default `minecraft:item/bucket`
- Overlay: sampled from `jsonreg:block/<id>_still` using the **vanilla water bucket's fluid silhouette** (sprite rows 3–5: narrow 4–11 / wide 3–12 / narrow 5–10), so the liquid sits inside the bucket instead of covering the white rim
- UV shares the geometry coordinates, so the fluid pattern (including animated textures) shows up inside the bucket; no `<id>_bucket.png` needed

### Required File Structure
```
.minecraft/resourcepacks/your_pack/ 
    └── assets/ 
        └── jsonreg/ 
            ├── textures/
            │ ├── item/
            │ │ └── test_item.png
            │ └── block/
            │     ├── test_block.png
            │     └── test_fluid_still.png
```

## License

MIT License