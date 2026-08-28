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
- Fluid buckets do not come with textures, so you need to provide your own textures.

### Important Notes
1. **Restart required**: Changes to configuration files take effect only after restarting the game
2. **First-run generation**: On first launch, example configuration files are automatically generated
3. **Error handling**: If JSON format is invalid, the mod logs errors and continues with an empty configuration – no game crash
4. **ID conflicts**: Blocks and their corresponding block items use the same ID; you cannot have a standalone item with the same ID at the same time
5. **Fluid properties**: `viscosity` affects flow speed (higher = slower, 1000 ≈ water); `density` below 1000 makes the fluid flow upward (light fluid / gas effect); `temperature` is not yet functional; **fluid fields left unset automatically fall back to vanilla water logic** (viscosity 1000, density 1000 flowing downward, temperature 300K)
6. **Resource priority**: Auto-generated resources can be overridden by traditional resource packs
7. **Fluid block textures**: Fluid blocks require texture files named `<id>_still.png` and `<id>_flow.png`

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
| `requires_tool` | Boolean | No | false | Whether a tool is required to drop the item |
| `has_item` | Boolean | No | true | Whether to automatically create the corresponding block item |
| `render_layer` | String | No | `solid` | Render layer: `solid` (default) / `cutoff` / `translucent`; the latter two behave like vanilla glass (light passes through, shared faces hidden between same blocks) |

`render_layer` options: `solid` (opaque, default), `cutoff` (cutout transparency, e.g., leaves), `translucent` (semi-transparent, e.g., glass). Blocks with `cutoff`/`translucent` behave like vanilla glass: light passes through completely, shared faces are hidden when adjacent to the same block, and faces facing them are drawn normally by adjacent solid blocks.

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
| `has_bucket_item` | Boolean | No | true | Whether to create a bucket item |
| `has_block` | Boolean | No | true | Whether to create a fluid block |

Fluid fields left unset automatically fall back to vanilla water logic: viscosity 1000 (flow interval of 5 ticks), density 1000 (flowing downward), luminosity 0, temperature 300K, and the bucket item and fluid block are created by default (`has_bucket_item`/`has_block` default to `true`).

Example:

```json
{
  "id": "molten_copper",
  "name": "Molten Copper",
  "viscosity": 2000,
  "density": 3000,
  "luminosity": 10,
  "temperature": 1370,
  "has_bucket_item": true,
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

### Fluid Block Models
- Based on the `minecraft:block/cube_all` template
- Automatically sets the texture path: `jsonreg:block/<id>_still`
- Automatically creates the fluid blockstate file

### Fluid Bucket Models
- Based on the `minecraft:item/generated` template
- Automatically sets the texture path: `jsonreg:item/<id>_bucket`

### Required File Structure
```
.minecraft/resourcepacks/your_pack/ 
    └── assets/ 
        └── jsonreg/ 
            ├── textures/
            │ ├── item/
            │ │ ├── test_item.png
            │ │ └── test_fluid_bucket.png
            │ └── block/
            │     ├── test_block.png
            │     └── test_fluid_still.png
```

## License

MIT License