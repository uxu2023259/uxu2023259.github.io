# TPSlag (Paper 1.21.4)

一个用于 Paper 1.21.4 的测试插件，提供两类功能：

1. **TPS 动态限速器**（`/tpslag`）
   - 命令：`/tpslag start [tps]`、`/tpslag stop`、`/tpslag status`、`/tpslag pos`
   - 目标：自动把服务器 TPS 压到指定值并尽量保持稳定。
   - 方式（同时进行，力度更高）：
     - 高强度 CPU 计算负载
     - 短时内存分配压力
     - 强制加载区块中的高频读写操作
     - 主线程额外纳秒级延迟（动态调节）
   - **在远离玩家的强制加载区块执行**（默认距离主世界出生点 512 个区块）。
   - 写入操作会记录并在下一轮/停止时恢复，支持热卸载回滚。
   - `start/status/pos` 会输出具体操作坐标（世界、区块、中心方块位置）。
   - `stop` 会彻底停止：取消任务、回滚方块、解除区块强制加载并清空运行状态。

2. **Bot 攻击场景模拟**（`/abot`）
   - 命令：`/abot [数量] [速度] [时间] [名称]`
   - 名称模板支持 `%X` 替换为 4 位随机数（例如 `Bot-%X` -> `Bot-0421`）
   - 使用**真实实体**（默认 `ZOMBIE`）作为 bot，而非盔甲架。
   - 通过定时移动/交互读取来制造“被 bot 冲击”的场景。
   - 创建 Bot 时会广播“加入消息”。
   - 可对接其他插件自定义加入信息：
     - 通过 Bukkit `ServicesManager` 注册 `com.tpslag.BotJoinMessageProvider`
     - 或监听 `SimulatedBotJoinMessageEvent` 修改/取消最终消息
   - ` /abot stop ` 可提前清理

## 配置（节选）

```yml
lag:
  startup-target-tps: 10.0
  world: world
  chunk-offset: 512
bots:
  entity-type: "ZOMBIE"
```

## 构建

```bash
mvn clean package
```

产物：`target/TPSlag-1.0.0.jar`
