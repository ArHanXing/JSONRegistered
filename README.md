### 本项目由 Deepseek-v4-pro 和 Qwen3 全权负责（不是

# JSON Registered

一个通过 JSON 配置文件快速注册物品、方块和流体的 Fabric 模组。  
无需编写代码，只需编辑配置文件即可向游戏添加自定义内容。

## ✨ 特性

- 🎯 **零代码开发**：仅需修改 JSON 配置文件
- 🔄 **运行时资源生成**：调教了好久AI才结束的，集成 运行时资源包，像 KubeJS 一样不需要繁琐复制默认模型
- 📦 **完整支持**：物品、方块、流体都可以哦！

## 在 AI 生成的 readme 之前的说明
### 需要安装  Fork 版本的 [ARRP](https://github.com/TriibuNupsik/ARRP)
### 目前的 Bug
对于方块的注册，请**手动在你的材质资源包内添加它的 BlockState 文件**。

一个示例的 BlockState 文件如下：
```json
// 位于 assets/jsonreg/blockstates/test_block.json
{
  "variants": {
    "": { "model": "jsonreg:block/test_block" }
  }
}
```

流体桶不自带材质，因此请自定义材质。

**高度不建议使用 has_block 的流体！** 创造的流体块有大量 Bug 且行为异常，建议仅作为合成原料使用。
### 注意事项
1. **重启生效**：修改配置文件后需要重启游戏才能生效
2. **首次生成**：首次启动时会自动生成示例配置文件
3. **错误处理**：如果 JSON 格式错误，模组会记录错误日志并继续使用空配置，不会导致游戏崩溃
4. **ID 冲突**：方块和对应的方块物品使用相同的 ID，不能同时存在同名的独立物品
5. **流体特性**： 流体的物理属性（粘度、密度等）目前仅作为数据存储
6. **资源优先级**：自动生成的资源可以被传统资源包覆盖
7. **流体方块纹理**：流体方块需要使用 `<id>_still.png` 命名的纹理文件
### 纹理要求
- **格式**：PNG 文件
- **尺寸**：推荐 16x16 或 32x32 像素
- **命名**：必须与配置文件中的 `id` 一致
## 安装

确保安装依赖：
   - Minecraft 1.21.1
   - Fabric Loader ≥ 0.19.2
   - Fabric API ≥ 0.116.11
   - Fabric Language Kotlin ≥ 1.13.11
   - [ARRP 的 1.21.1 Fork 版本](https://github.com/TriibuNupsik/ARRP)

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

示例：

```json
{
  "id": "reinforced_glass",
  "name": "强化玻璃",
  "hardness": 2.5,
  "resistance": 10.0,
  "requires_tool": false,
  "has_item": true
}
```

### 流体（`fluids`）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | 字符串 | 是 | - | 流体唯一标识符 |
| `name` | 字符串 | 否 | 与 `id` 相同 | 流体显示名称 |
| `viscosity` | 整数 | 否 | 1000 | 粘度（影响流动速度） |
| `density` | 整数 | 否 | 1000 | 密度（影响流动方向） |
| `luminosity` | 整数 | 否 | 0 | 发光等级（0-15） |
| `temperature` | 整数 | 否 | 300 | 温度（开尔文） |
| `has_bucket_item` | 布尔值 | 否 | true | 是否创建桶物品 |
| `has_block` | 布尔值 | 否 | true | 是否创建流体方块 |


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
  "has_block": true }
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
- 基于 `minecraft:item/generated` 模板
- 自动设置纹理路径：`jsonreg:item/<id>_bucket`

### 所需文件结构
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

## 还有点说明

本项目使用 JITPack.io 加载了 [ARRP 的 1.21.1 Fork 版本](https://github.com/TriibuNupsik/ARRP)，它还没有被合并x

## 📄 许可证

MIT License