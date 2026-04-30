### 本项目由 Deepseek-v4-pro 全权负责（不是
# JSON Registered

实现通过 JSON 配置文件注册物品、方块、流体的 Fabric 模组。  
无需编写任何代码，只需编辑配置文件即可向游戏添加自定义内容。

~~一切的原因都是因为Content Tweaker和KubeJS都不支持1.21.1 Fabric。~~
~~以下说明都是 AI 生成的。~~

## 安装
1. 将模组文件放入 `mods/` 文件夹。
2. 确保已安装依赖：
    - Fabric Loader ≥ 0.15.11
    - Fabric API ≥ 0.116.11
    - Fabric Language Kotlin ≥ 1.13.1
    - 我想你需要安装一个 OpenLoader
3. 启动游戏，模组会自动在 `config/jsonreg_entries.json` 生成示例配置文件。

## 配置文件结构
文件位置：`config/jsonreg_entries.json`  
根对象包含三个数组：
```json
{
  "items": [],
  "blocks": [],
  "fluids": []
}
```
每个条目均为 JSON 对象，字段说明如下。

### 物品（`items`）
| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | 字符串 | 是 | - | 物品注册名（命名空间自动为 `jsonreg`），最终为 `jsonreg:你的id` |
| `name` | 字符串 | 否 | `id` 的值 | 显示名称（目前仅用于文档，实际名称需通过语言文件覆盖） |
| `max_count` | 整数 | 否 | 64 | 最大堆叠数量 |

示例：
```json
{
  "id": "copper_plate",
  "max_count": 64
}
```

### 方块（`blocks`）
| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | 字符串 | 是 | - | 方块注册名，同时会生成对应的物品形式（若 `has_item` 为 true） |
| `name` | 字符串 | 否 | `id` 的值 | 显示名称（同物品） |
| `hardness` | 浮点数 | 否 | 1.5 | 方块硬度（影响挖掘速度） |
| `resistance` | 浮点数 | 否 | 6.0 | 爆炸抗性 |
| `requires_tool` | 布尔值 | 否 | false | 是否需要工具才能掉落（类似矿石） |
| `has_item` | 布尔值 | 否 | true | 是否自动注册对应的 BlockItem（若为 false，则无法在物品栏获得该方块） |

示例：
```json
{
  "id": "reinforced_glass",
  "hardness": 2.5,
  "resistance": 10.0,
  "requires_tool": false,
  "has_item": true
}
```

### 流体（`fluids`）
| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | 字符串 | 是 | - | 流体注册名。会自动生成 `id`（静止）和 `id_flowing`（流动）两个流体 |
| `name` | 字符串 | 否 | `id` 的值 | 显示名称 |
| `viscosity` | 整数 | 否 | 1000 | 粘度（影响流动速度） |
| `density` | 整数 | 否 | 1000 | 密度（影响流动方向） |
| `luminosity` | 整数 | 否 | 0 | 亮度（目前保留，但流体无方块形态不生效） |
| `temperature` | 整数 | 否 | 300 | 温度（用于配方等） |
| `has_bucket_item` | 布尔值 | 否 | true | 是否注册对应的桶物品（`id_bucket`） |

**特别说明：**   
如果 `has_bucket_item` 为 true，你会得到一个桶物品，但桶无法倒出方块（右键地面不会放置流体），仅可用于合成或其他模组的机器输入。

示例：
```json
{
  "id": "molten_copper",
  "viscosity": 2000,
  "density": 3000,
  "temperature": 1370,
  "has_bucket_item": true
}
```

## 外观配置
默认情况下物品、方块、流体桶均显示为紫黑方块（丢失模型）。若要提升体验，你需要自行提供资源包，所需文件如下：

### 物品
- 模型：`assets/jsonreg/models/item/<id>.json`
- 纹理：`assets/jsonreg/textures/item/<id>.png`
- 简单平面贴图模型内容：
  ```json
  {
    "parent": "minecraft:item/generated",
    "textures": {
      "layer0": "jsonreg:item/<id>"
    }
  }
  ```

### 方块
- 方块状态：`assets/jsonreg/blockstates/<id>.json`
- 方块模型：`assets/jsonreg/models/block/<id>.json`
- 方块物品模型（自动调用方块模型）：`assets/jsonreg/models/item/<id>.json`
- 纹理：`assets/jsonreg/textures/block/<id>.png`
- 最简全方块六面相同纹理：方块模型使用 `"parent": "minecraft:block/cube_all"` 并指定 `"all"` 纹理。

### 流体桶
- 流体无法放置，所以桶无法在 JEI 中显示液体颜色。若需显示桶内液体颜色，则**必须注册液体方块**。
## 注意事项
1. **所有注册均在游戏启动时完成，修改配置文件后需重启游戏生效。**
2. 配置文件首次启动时自动生成，包含一个示例条目。如果 JSON 格式错误，模组会记录错误并继续使用空配置，不会崩溃。
3. 方块自动注册的物品与方块使用同一 ID（如 `jsonreg:test_block`），因此无法同时存在同名的独立物品。
4. 流体没有实现实际的流动逻辑，`viscosity` 和 `density` 仅作为参数存储，可能被其他模组利用，但在原版世界中无效果。
5. 如果你需要让物品/方块拥有真正的纹理，请制作资源包。

## 许可
MIT License
