[中文](README.md) | [English](README-en.md)


# JSON Registered

给整合包作者，不过也只有我会在这种版本制作整合包。
一个通过 JSON 配置文件快速注册物品、方块和流体的 Fabric 模组，无需编写代码

## 特性

- 集成 **运行时资源包**，你只需要复制材质本身，复刻 KubeJS 的注册体验
- 支持物品、流体、方块、玻璃（？）
- 可自定义，支持气体（气体流动形式，不是 Mekanism 的化学品）

## 说明
- 前置是 **YARRP Mod**
- 流体桶材质是**自动生成**的，默认用原版铁桶当底图，再从 `<id>_still` 流体纹理上截一块贴到桶口。此外可以自定义桶的材质。
- 语言键文件默认是 `zh_cn`

确保安装依赖：
   - Minecraft 1.21.1
   - Fabric Loader ≥ 0.19.2
   - Fabric API ≥ 0.116.11
   - Fabric Language Kotlin ≥ 1.13.11
   - YARRP ≥ 0.2.0

### 纹理要求
- **格式**：PNG 文件
- **尺寸**：推荐 16x16 或 32x32 像素
- **命名**：必须与配置文件中的 `id` 一致
- **透明度**：`render_layer` 为 `cutoff`/`translucent` 的方块请使用带 alpha 通道的 PNG 纹理

## 配置文件结构

文件位置：`.minecraft/config/jsonreg_entries.json`

根对象包含三个数组：

```json
{
  "items": [],
  "blocks": [],
  "fluids": []
}
```

每个条目均为 JSON 对象，字段说明如下。

### 物品配置（`items`）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | 字符串 | 是 | - | 物品唯一标识符，最终注册为 `jsonreg:<id>` |
| `name` | 字符串 | 否 | 与 `id` 相同 | 物品的显示名称（支持中文等多语言） |
| `max_count` | 整数 | 否 | 64 | 最大堆叠数量（1-64） |


示例：

```json
{
  "id": "copper_plate", 
  "name": "铜板",
  "max_count": 64
}
```

### 方块（`blocks`）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | 字符串 | 是 | - | 方块唯一标识符 |
| `name` | 字符串 | 否 | 与 `id` 相同 | 方块的显示名称 |
| `hardness` | 浮点数 | 否 | 1.5 | 方块硬度（影响挖掘速度） |
| `resistance` | 浮点数 | 否 | 6.0 | 爆炸抗性 |
| `requires_tool` | 布尔值 | 否 | false | 是否需要工具才能掉落物品 |
| `has_item` | 布尔值 | 否 | true | 是否自动创建对应的方块物品 |
| `render_layer` | 字符串 | 否 | `solid` | 渲染层：`solid`（默认）/ `cutoff` / `translucent`，后两者行为同原版玻璃（透光、同种相邻隐藏共享面） |

`render_layer` 可选值：`solid`（不透明，默认）、`cutoff`（剔除透明，如树叶）、`translucent`（半透明，如玻璃）。`cutoff`/`translucent` 方块与原版玻璃行为一致：光照完全穿透、与同种方块相邻时隐藏共享面、与普通方块相邻时正常显示。

示例：

```json
{
  "id": "reinforced_glass",
  "name": "强化玻璃",
  "hardness": 2.5,
  "resistance": 10.0,
  "requires_tool": false,
  "has_item": true,
  "render_layer": "translucent"
}
```

### 流体（`fluids`）

| 字段 | 类型 | 必填 | 默认值 | 说明                        |
|------|------|------|--------|---------------------------|
| `id` | 字符串 | 是 | - | 流体唯一标识符                   |
| `name` | 字符串 | 否 | 与 `id` 相同 | 流体显示名称                    |
| `viscosity` | 整数 | 否 | 1000 | 粘度（影响流动速度，值越大越慢，1000≈原版水） |
| `density` | 整数 | 否 | 1000 | 密度（<1000 向上流动，≥1000 向下流动） |
| `luminosity` | 整数 | 否 | 0 | 发光等级（0-15）                |
| `temperature` | 整数 | 否 | 300 | 温度（开尔文，暂未生效） |
| `has_bucket_item` | 布尔值 | 否 | true | **已废弃**：桶物品与桶模型已总是生成（见下方说明），该字段保留仅为兼容旧配置 |
| `has_block` | 布尔值 | 否 | true | 是否创建流体方块                  |
| `bucket_texture` | 字符串 | 否 | `minecraft:item/bucket` | 桶模型用的底桶纹理（16x16） |

流体字段未填写时会自动回退到原版水的逻辑：粘度 1000（流动间隔 5 tick）、密度 1000（向下流动）、发光 0、温度 300K，且默认生成流体方块（`has_block` 缺省为 `true`）。

> **关于桶物品**：只要 `has_block` 为 `true`，就会注册 `jsonreg:<id>_bucket`，同时生成桶模型和语言条目。
> 原因：`FluidBlock` 装桶时依赖 `fluid.getBucketItem()`，如果它返回空气，原版桶/科技复兴流体单元会在删除流体方块后拿到空栈，
> 表现为“收了流体但不返还带流体的桶”。桶模型默认由**原版铁桶 + 从流体纹理截取的一块**动态拼出，因此不再需要手画 `<id>_bucket.png`。
> 反过来，`has_block=false` 的流体不会注册桶——没有流体方块时桶既装不到也倒不出，只会白白被消耗。


示例：

```json
{
  "id": "molten_copper",
  "name": "熔融铜",
  "viscosity": 2000,
  "density": 3000,
  "luminosity": 10,
  "temperature": 1370,
  "has_bucket_item": true,
  "has_block": true
}
```
## 🎨 自动生成资源

模组会自动生成以下内容：

### 本地化文件
- zh_cn 翻译文件
- 物品格式：`item.jsonreg.<id>` → `<name>`
- 方块格式：`block.jsonreg.<id>` → `<name>`
- 流体桶格式：`item.jsonreg.<id>_bucket` → `<name> Bucket`

### 物品模型
- 基于 `minecraft:item/generated` 模板
- 自动设置 `layer0` 纹理路径：`jsonreg:item/<id>`

### 方块模型
- 基于 `minecraft:block/cube_all` 模板（六面相同纹理）
- 自动设置 `all` 纹理路径：`jsonreg:block/<id>`
- 自动创建方块状态文件（blockstate）
- 自动创建方块物品模型

### 流体方块模型
- 基于 `minecraft:block/cube_all` 模板
- 自动设置纹理路径：`jsonreg:block/<id>_still`
- 自动创建流体方块状态文件

### 流体桶模型
- 生成的是扁平物品模型（不依赖模板）
- 底层：`bucket_texture` 指定的底桶纹理，默认 `minecraft:item/bucket`
- 覆盖层：按**原版水桶的液面轮廓**（sprite 第 3~5 行，上窄 4~11 / 中宽 3~12 / 下窄 5~10）从 `jsonreg:block/<id>_still` 对应位置取样贴上，所以液体正好落在桶内、不会盖住白色桶沿
- UV 与几何同坐标，流体图案（含动画纹理）会原样出现在桶里，不需要手画 `<id>_bucket.png`

### 所需文件结构
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

## 许可证

MIT License