# FastBuilderPro

FastBuilderPro 是一个基于 Paper 1.21 的建筑辅助插件：
- 支持通过选区复制结构；
- 支持保存/读取 `.schem` 结构文件；
- 提供粒子预览 + 材料确认 + 自动建造流程；
- 支持 `/fb undo` 撤销最近一次建造并返还可校验消耗；
- 在材料不足时自动暂停，并支持无序续建（优先使用背包已有材料）。

## 1. 技术栈与运行环境

- Java 17
- Paper API `1.21.4-R0.1-SNAPSHOT`
- WorldEdit Bukkit `7.3.11`
- Maven 打包（`jar`）

## 2. 核心模块速览

### 2.1 插件入口

`FastBuilderPro` 负责：
- 初始化管理器（选区、预览、结构文件）；
- 注册 `/fb` 命令与 GUI 事件监听；
- 维护玩家级状态（复制缓存、活跃建造任务、待确认旋转角）；
- 发起预览流程与建造流程。

### 2.2 选区与复制

`SelectionManager` 提供：
- `pos1/pos2` 选点（使用玩家脚下方块位置）；
- 粒子边框可视化选区；
- 把选区内非空气方块复制为 `StructureCache`。

复制时会忽略容器方块的库存 NBT（保留方块类型/状态）。

### 2.3 结构文件

- `StructureSaver`：把选区保存为 Sponge `.schem`；
- `WorldEditStructureLoader`：读取 `.schem/.schematic` 为内存缓存；
- `StructureFileManager`：插件启动或重载时扫描 `plugins/FastBuilderPro/str` 目录。

### 2.4 预览系统

`PreviewManager` 会按照指定旋转角（0/90/180/270）周期性生成粒子，
并冻结预览基准点（Confirm 前不会随玩家移动而变）。

### 2.5 自动建造

`BuildSession` 是单个玩家的一次建造任务：
- 按配置的 Tick 间隔与每 Tick 放置数量推进；
- 生存模式下会实际消耗背包材料；创造模式不消耗；
- 目标位置非空气时跳过该方块；
- 材料不足会暂停并弹出缺料 GUI；
- 提供 BossBar 与进度 GUI。

## 3. 命令说明

主命令：`/fb`

- `/fb pos1`：设置选区点 1（脚下方块）
- `/fb pos2`：设置选区点 2（脚下方块）
- `/fb copy`：复制当前选区到内存
- `/fb save <名称>`：将当前选区保存为 `str/<名称>.schem`
- `/fb list`：列出已加载结构名
- `/fb preview <结构> [角度]`：仅预览，不触发确认 GUI
- `/fb build <结构> [角度]`：读取结构并预览，预览结束后弹确认 GUI
- `/fb paste [角度]`：对“上次复制的结构”直接进入预览+确认流程
- `/fb progress`：查看当前建造任务进度
- `/fb undo`：撤销最近一次建造并返还可校验消耗
- `/fb reload`：重载配置与结构文件

角度仅支持：`0 / 90 / 180 / 270`。

结构文件读取支持 WorldEdit 可识别格式（含 `.schem` / `.schematic`，以及服务端环境支持时的 `.litematic`）。

## 4. GUI 流程

### 4.1 材料确认 GUI（`§8材料确认`）

- 展示需求与当前拥有数量；
- 支持分页；
- 点击“开始建造”后，按预览基准位置正式开工；
- 点击“取消建造”会关闭并停止预览。

### 4.2 建造进度 GUI（`§8建造进度`）

- 实时显示完成百分比、已放置/总数；
- 可点击取消当前建造任务。

### 4.3 材料不足 GUI（`§c材料不足`）

- 展示从当前进度继续所缺的材料；
- 补齐材料后可“继续建造”；
- 或“取消建造”终止任务。

## 5. 权限节点

`plugin.yml` 中定义：
- `fb.*`（默认 OP，包含常用子权限）
- `fb.use`（默认 OP）

代码中额外对 `/fb reload` 检查 `fb.reload`。

## 6. 配置项（`config.yml`）

```yaml
build:
  tick-interval: 1
  blocks-per-tick: 1
  max-blocks: 200000

preview:
  seconds: 10
```

说明：
- `tick-interval`：建造任务执行周期（tick）；
- `blocks-per-tick`：每次周期尝试放置方块数；
- `max-blocks`：单次建造允许的最大方块总量；
- `preview.seconds`：`build/paste` 进入确认 GUI 前的预览时长。

## 7. 构建与部署

```bash
mvn clean package
```

打包产物在 `target/FastBuilderPro-1.0.jar`（版本号取决于 `pom.xml`）。
将其放入服务器 `plugins` 目录并重启。

## 8. 典型使用流程

### 流程 A：从世界中复制后直接施工

1. 站到两个角点分别执行 `/fb pos1`、`/fb pos2`
2. 执行 `/fb copy`
3. 执行 `/fb paste 90`（角度可省略）
4. 等待预览结束，确认材料后点击开始

### 流程 B：保存为结构文件后复用

1. `/fb pos1` + `/fb pos2`
2. `/fb save house_a`
3. `/fb reload`（或重启）
4. `/fb build house_a 180`

---

如需二次开发，建议从以下入口阅读代码：
`FastBuilderPro` → `FBCommand` → `BuildSession`/`PreviewManager`/`SelectionManager`。
