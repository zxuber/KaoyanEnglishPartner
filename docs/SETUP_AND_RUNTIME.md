# SETUP_AND_RUNTIME.md - 跨平台环境与联调说明

> 最后更新：2026-06-22
> 用途：统一说明 macOS / Windows 下的运行前置条件、配置入口、真机联调方式。

---

## 1. 设计目标

当前工程已调整为“代码跨平台、差异留在配置层”的结构：

- 后端不再把 Python 命令、ffmpeg 路径、数据库账号、服务监听地址写死在代码里
- 前端不再把 `http://localhost:8080` 写死在页面和请求层
- 真机联调支持三种模式：`local` / `lan` / `tunnel`

---

## 2. 后端配置入口

后端统一使用环境变量驱动，基础配置文件见：

- `backend/src/main/resources/application.yml`

关键环境变量：

| 变量名 | 作用 | 默认值 |
|--------|------|--------|
| `SERVER_PORT` | SpringBoot 端口 | `8080` |
| `SERVER_ADDRESS` | SpringBoot 监听地址 | `0.0.0.0` |
| `DB_URL` | MySQL 连接串 | `jdbc:mysql://localhost:3306/kaoyan_peipao?...` |
| `DB_USERNAME` | MySQL 用户名 | `root` |
| `DB_PASSWORD` | MySQL 密码 | `root` |
| `APP_STT_PYTHON_COMMAND` | Python 命令 | 空，运行时自动探测 |
| `APP_STT_SCRIPT_PATH` | STT Python 脚本路径 | `scripts/stt.py` |
| `APP_STT_FFMPEG_PATH` | ffmpeg 命令或绝对路径 | `ffmpeg` |
| `APP_STT_TIMEOUT_SECONDS` | STT 超时秒数 | `10` |
| `WX_MINIAPP_APPID` | 微信小程序 AppID | 配置文件默认值 |
| `WX_MINIAPP_SECRET` | 微信小程序 Secret | 配置文件默认值 |
| `DEEPSEEK_API_KEY` | DeepSeek Key | 配置文件默认值 |
| `DEEPSEEK_BASE_URL` | DeepSeek Base URL | `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | DeepSeek 模型名 | `deepseek-chat` |

### DeepSeek Key 策略

仓库中不再保留真实的 DeepSeek API Key。

- `application.yml` 中的 `deepseek.api-key` 只保留环境变量入口，不提供真实默认值
- 新机器拉下代码后，如果没有先设置 `DEEPSEEK_API_KEY`，LLM 相关功能将不可用
- 这是为了避免真实密钥进入 Git 提交历史并被 GitHub Push Protection 拦截

macOS 示例：

```bash
export DEEPSEEK_API_KEY="your-real-key"
```

Windows PowerShell 示例：

```powershell
$env:DEEPSEEK_API_KEY="your-real-key"
```

### Python 兼容策略

`SpeechService` 现在采用以下规则：

- 如果设置了 `APP_STT_PYTHON_COMMAND`，就严格使用这个值
- 否则自动探测
  - Windows：优先 `python`，其次 `py`
  - macOS / Linux：优先 `python3`，其次 `python`

这意味着同一套代码在 Win/Mac 上都能直接工作，只要目标机器上存在可执行的 Python。

---

## 3. 前端接口地址模式

前端新增统一配置入口：

- `frontend/src/config/api.ts`

支持三种模式：

| 模式 | 用途 | 示例 |
|------|------|------|
| `local` | 本机开发 / 模拟器调试 | `http://localhost:8080/api/v1` |
| `lan` | 手机与电脑同一局域网真机调试 | `http://192.168.1.100:8080/api/v1` |
| `tunnel` | 内网穿透 / 远程联调 | `https://your-tunnel.example.com/api/v1` |

前端环境变量模板见：

- `frontend/.env.example`

可在 `frontend/.env.local` 中自行填写：

```dotenv
VITE_API_MODE=lan
VITE_API_BASE_URL_LOCAL=http://localhost:8080/api/v1
VITE_API_BASE_URL_LAN=http://192.168.1.100:8080/api/v1
VITE_API_BASE_URL_TUNNEL=https://your-tunnel.example.com/api/v1
```

### 运行时覆盖

前端还支持运行时覆盖地址，存储键为：

- `apiBaseUrlOverride`

对应能力在：

- `setApiBaseUrlOverride(url)`
- `clearApiBaseUrlOverride()`
- `getApiRuntimeSummary()`

便于后续做调试开关页，而不需要改业务代码。

---

## 4. 真机联调推荐方案

### 方案 A：局域网模式（推荐）

适用场景：手机和开发机在同一 Wi-Fi。

要求：

1. 后端监听 `0.0.0.0`
2. 前端 `VITE_API_MODE=lan`
3. `VITE_API_BASE_URL_LAN` 指向电脑局域网 IP
4. Mac / Windows 防火墙允许 8080 入站

优点：

- 配置简单
- 延迟低
- 最适合日常真机开发

当前已验证示例：

- 开发机 IP：`172.30.17.43`
- 后端地址：`http://172.30.17.43:8080/api/v1`
- 对应前端本地文件：`frontend/.env.local`

### 方案 B：内网穿透模式

适用场景：手机不在同一局域网，或需要远程演示。

要求：

1. 使用 `ngrok` / `frp` / `cpolar` 等工具暴露本机 8080
2. 前端 `VITE_API_MODE=tunnel`
3. `VITE_API_BASE_URL_TUNNEL` 指向公网 HTTPS 地址

优点：

- 不依赖同一网络
- 更接近线上访问链路

---

## 5. 跨平台启动参考

### macOS

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"
export APP_STT_PYTHON_COMMAND=python3
export DEEPSEEK_API_KEY="your-real-key"

cd backend
mvn spring-boot:run
```

### Windows PowerShell

```powershell
$env:APP_STT_PYTHON_COMMAND="python"
$env:DEEPSEEK_API_KEY="your-real-key"
cd backend
mvn spring-boot:run
```

如果 Windows 机器上只有 `py` 可用：

```powershell
$env:APP_STT_PYTHON_COMMAND="py"
```

---

## 6. 新会话必读文件

后续任何新会话如果要快速理解项目设计，至少应先读这几份：

1. `docs/PROJECT_CONTEXT.md`
2. `docs/SETUP_AND_RUNTIME.md`
3. `docs/TROUBLESHOOTING.md`
4. `docs/PRD.md`

这四份组合起来，已经能重建当前项目的运行模型、技术选型、联调方式和已知边界。

---

## 7. 当前运行结论（2026-06-22）

截至 2026-06-22，项目已在当前 macOS 机器上验证通过：

- MySQL 可启动并可连接
- `kaoyan_peipao` 已建库
- `user` / `word` / `word_progress` 已建表
- `word` 表已导入 6,547 条词库数据
- SpringBoot 后端可正常启动
- 微信小程序前端可正常构建
- `lan` 模式真机联调已验证通过
