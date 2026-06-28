# 一键式安装包打包说明

本文档说明如何生成面向普通用户的 Windows/macOS 一键运行包。运行包内置 Java 运行时，并使用本地文件数据库，最终用户无需安装 Java、Node.js、Maven 或 MySQL。

## 方案说明

- 开发/生产环境仍默认使用 `application.properties` 中的 MySQL 配置。
- 一键运行包使用 `standalone` Profile：
  - 数据库：H2 文件数据库，启用 MySQL 兼容模式。
  - 数据目录：启动脚本设置为运行包内的 `data/`。
  - 建表：Hibernate 根据实体自动维护。
  - 初始数据：默认由 `StandaloneDemoDataInitializer` 首次启动写入 README 中列出的管理员、车主、技师、钣喷技师四个默认账号；如果打包时使用 `--sync-mysql-data`，则优先导入包内 MySQL 数据快照。
- 前端 `frontend/dist` 会在 Maven 打包时写入后端 jar 的 `static/` 目录，由 Spring Boot 统一托管。
- 现有 `application-secret.properties` 会随后端 jar 一起进入运行包，AI API Key 无需普通用户配置。

## 构建机要求

这些要求只针对负责出包的开发者，不针对最终用户：

- JDK 17+
- Node.js / npm
- Bash、curl、zip、unzip、tar
- 可访问互联网，用于下载各平台 Temurin JRE 17

Windows 包含 `启动系统.bat`、`停止系统.bat`、`使用说明.txt` 等中文文件名。打包脚本会使用 JDK 自带的 `jar` 工具生成 Windows zip，确保 ZIP 条目写入 UTF-8 文件名标志，避免用户在 Windows 解压后看到乱码文件名。发布 Windows 包时不要用普通 `zip -qr` 手工重新压缩目录。

## 生成安装包

在项目根目录执行：

```bash
chmod +x scripts/package-release.sh
scripts/package-release.sh --platform all
```

也可以只生成某个平台：

```bash
scripts/package-release.sh --platform windows-x64
scripts/package-release.sh --platform macos-arm64
scripts/package-release.sh --platform macos-x64
scripts/package-release.sh --platform current
```

如果希望安装包内初始数据同步当前项目 MySQL（默认读取 `localhost:3306/car_repair`，账号 `root`，密码 `79Haolubenwei`），执行：

```bash
scripts/package-release.sh --platform all --sync-mysql-data
```

可通过 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD` 环境变量覆盖连接信息。脚本会排除 `flyway_schema_history` 与登录态 `auth_refresh_token`，将其余业务表导出为 `app/mysql-snapshot.sql`，一键包首次启动时导入到本地 H2。

输出目录：

```text
dist/installers/
  fangxingwei-ai-windows-x64.zip
  fangxingwei-ai-macos-arm64.zip
  fangxingwei-ai-macos-x64.zip
```

## 用户使用方式

将对应平台的 zip 发给用户。用户解压后：

- Windows：双击 `启动系统.bat`
- macOS：双击 `启动系统.command`；如果系统提示无法打开，请右键选择“打开”

启动后浏览器会打开：

```text
http://localhost:8080
```

默认演示账号：

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `123456` |
| 车主 | `user` | `123456` |
| 技师 | `tech` | `123456` |
| 钣喷技师 | `body` | `123456` |

## 数据与重置

运行包数据保存在解压目录的 `data/` 下。需要恢复到初始状态时：

1. 先运行 `停止系统` 脚本。
2. 删除 `data/` 目录。
3. 再运行 `启动系统` 脚本。

如果该包是通过 `--sync-mysql-data` 生成的，重置后会重新导入包内 MySQL 快照；否则会恢复到仅包含默认账号的初始状态。

## MySQL 说明

一键运行包不要求最终用户安装 MySQL。项目原有 MySQL schema、Flyway 迁移和 SQL 种子文件仍用于开发、测试和正式部署；打包版为了降低安装门槛，改用本地文件数据库承载单机演示/轻量使用场景。

如果后续要做门店长期生产部署，建议仍使用独立 MySQL，并按 `SQL/README.md` 初始化和维护数据库。

## 发布注意事项

- macOS 未签名包可能触发 Gatekeeper 提示，当前方案通过右键“打开”绕过；正式商业发布建议增加开发者证书签名和 notarization。
- Windows 可能触发 SmartScreen 提示，正式发布建议使用签名证书。
- Windows zip 必须保留 UTF-8 文件名编码标志；请直接使用 `scripts/package-release.sh --platform windows-x64` 生成的 zip，不要在 macOS/Linux 上手动重新压缩发布目录。
- 发布前确认 Windows 包不包含本机运行态文件，例如 `data/app.pid`、`data/database/*.db`、`logs/*.log`。打包脚本会在压缩前清空 `data/` 与 `logs/` 目录，只保留空目录。
- 若用户机器上 `8080` 端口被占用，可在启动前设置环境变量 `APP_PORT`，或在启动脚本中调整端口。
