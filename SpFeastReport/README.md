# SpFeastReport

一个面向 Purpur 1.21.x 的举报插件，界面风格参考 Hypixel 举报菜单，并加入了分类记录、管理员查看页和后续网页读取所需的数据结构。

## 版本

- 当前版本：`26.1.0`
- 版本策略：按年版本号（CalVer）
- 规则：
  - `26.1.0` = 2026 年第 1 个正式版本
  - `26.2.0` = 2026 年第 2 个功能版本
  - `26.2.1` = 对 `26.2.0` 的修复版
- 建议 `SpFeastReport`、`spfeastApi`、`spfeastBans` 保持同一主版本代号，方便联动排查

## 当前功能

- `/report <player>` 打开举报菜单
- 举报分类支持二次确认页面
- 每次举报按分类写入独立 yml 文件
- 自动生成总索引文件，方便后续扩展
- `/reportcheck` 管理员查看举报记录
- 支持分类查看、分页查看、详情查看
- 支持只看待处理记录 / 查看全部记录
- 支持将记录标记为 `No Error`
- 支持有坐标的记录直接 TP 到保存位置
- 支持 LuckPerms 权限拆分，方便区分 helper / mod

## 环境要求

- 服务端：Purpur 1.21.x
- Java：21

## 命令

- `/report <player>`：打开举报菜单
- `/reportcheck`：打开举报管理菜单

## 权限

- `spfeastreport.command.report`：允许使用 `/report`
- `spfeastreport.command.reportcheck`：允许使用 `/reportcheck`
- `spfeastreport.review.view`：允许查看举报管理菜单
- `spfeastreport.review.teleport`：允许传送到举报记录保存的坐标
- `spfeastreport.review.action`：允许使用处理按钮
- 汇总节点：`spfeastreport.command.*`、`spfeastreport.review.*`、`spfeastreport.*`

## 推荐的 LuckPerms 分配方式

- helper：
  - `spfeastreport.command.reportcheck`
  - `spfeastreport.review.view`
  - `spfeastreport.review.teleport`
- mod：在 helper 的基础上再加 `spfeastreport.review.action`

## 数据文件结构

```text
plugins/SpFeastReport/
├─ categories.yml
└─ reports/
   ├─ index.yml
   ├─ chat_abuse_scam.yml
   ├─ cheating_hacking.yml
   ├─ bad_name.yml
   └─ ...
```

- `categories.yml`：分类配置，可控制是否启用、是否保存坐标等
- `reports/*.yml`：每个举报类型一个文件，每条举报单独记录
- `reports/index.yml`：总索引文件，方便后续网页或其它插件读取

## 记录内容

- 举报 ID
- 举报编号
- 举报类型 key / 标题
- 举报时间
- 举报者名称 / UUID
- 被举报者名称 / UUID
- 坐标信息（仅对启用了坐标保存的分类）
- 处理状态、处理人、处理时间

## 当前处理状态

- `pending`
- `no_error`
- `banned`
- `muted`

## 已完成的管理逻辑

- 默认只显示待处理记录
- 可切换为查看全部记录
- `No Error` 后记录仍保留在 yml 中
- 默认管理页不再显示已处理记录
- 在“查看全部记录”模式下仍可看到处理结果

## 后续计划

- `/report` 无参数页面
- `/wdr` / `/wdreport` 快速进入指定分类确认页
- 更完整的 ban / mute 联动规则
- 网页端读取举报记录

## 构建

```powershell
.\gradlew.bat build
```

生成的 jar 位于：

```text
build/libs/SpFeastReport-26.1.0.jar
```
