# SETUP_AND_RUNTIME.md - 跨平台环境与联调说明

> 最后更新：2026-06-25
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
| `DEEPSEEK_API_KEY` | DeepSeek Key | 空 |
| `DEEPSEEK_BASE_URL` | DeepSeek Base URL | `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | DeepSeek 模型名 | `deepseek-chat` |
| `APP_JWT_SECRET` | JWT 签名密钥（Base64） | 本地开发默认值 |
| `APP_JWT_EXPIRE_DAYS` | JWT 过期天数 | `30` |

### DeepSeek Key 策略

仓库中不再保留真实的 DeepSeek API Key。

- `application.yml` 中的 `deepseek.api-key` 只保留环境变量入口，不提供真实默认值
- 新机器拉下代码后，如果没有先设置 `DEEPSEEK_API_KEY`，LLM 相关功能将不可用
- 这是为了避免真实密钥进入 Git 提交历史并被 GitHub Push Protection 拦截

macOS 示例：

```bash
export DEEPSEEK_API_KEY="your-real-key"
```

如果你是通过终端启动后端，这样设置即可生效。

如果你是通过 `Trae / IDEA / VS Code Java Run` 这类 GUI 运行按钮启动后端，需要额外注意：

- 这类启动方式不一定会继承 `~/.zshrc`
- 也不一定会继承 `~/.zprofile`
- 最稳妥的方式是把 `DEEPSEEK_API_KEY` 直接配进对应的 Run Configuration / Environment Variables

否则会出现：

- 单词翻译部分可用（命中本地词典或词库）
- 短句翻译、阅读 AI 教练、学习方案生成返回 LLM 401

因为这些功能一定依赖真实的 DeepSeek 调用。

Windows PowerShell 示例：

```powershell
$env:DEEPSEEK_API_KEY="your-real-key"
```

### 数据库初始化脚本

完整初始化入口：

- `backend/docs/database/init_all.sql`

当前会按顺序执行：

- `001_create_user_table.sql`
- `002_create_word_tables.sql`
- `003_reassign_word_units.sql`
- `004_expand_user_profile.sql`
- `005_create_mistake_asset_tables.sql`
- `006_seed_mistake_assets.sql`

其中 `005/006` 是 3 tab 分支新增的误解本资产库初始化：

- `mistake_asset_library`：全局资产库
- `mistake_asset_progress`：用户对资产的 `done` 状态
- 默认原始数据：`写作表达 / 固定搭配 / 易混词` 各 100 条

新电脑重建数据库时，执行完整初始化脚本即可同步这些原始数据：

```bash
cd backend/docs/database
mysql -u root -p kaoyan_peipao < init_all.sql
```

如果数据库已经存在，只想补误解本资产库：

```bash
cd backend/docs/database
mysql -u root -p kaoyan_peipao < migrations/005_create_mistake_asset_tables.sql
mysql -u root -p kaoyan_peipao < migrations/006_seed_mistake_assets.sql
```

### 微信登录与 JWT

当前前后端已经接通正式登录主链路：

- 前端：`uni.login()`
- 后端：`POST /api/v1/auth/wx-login`
- 返回：`token + userId + onboardingDone`

说明：

- 小程序真机或微信开发者工具下，首页会优先尝试微信登录
- 登录成功后，本地保存 JWT，后续请求统一走 `Authorization: Bearer ...`
- 如果 token 失效，前端会清空本地会话并重新回到首页触发登录

如果要在新机器上稳定验证登录，请确保以下配置可用：

- `WX_MINIAPP_APPID`
- `WX_MINIAPP_SECRET`
- Java 17 运行环境

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
| `tunnel` | 公网 HTTPS（内网穿透 / 线上域名） | `https://api.peipaoenglish.cn/api/v1` |

前端环境变量模板见：

- `frontend/.env.example`

可在 `frontend/.env.local` 中自行填写。当前默认建议值是 `lan`，优先走同一局域网真机联调；如只在本机模拟器开发，再切回 `local`：

```dotenv
VITE_API_MODE=lan
VITE_API_BASE_URL_LOCAL=http://localhost:8080/api/v1
VITE_API_BASE_URL_LAN=http://172.30.17.43:8080/api/v1
VITE_API_BASE_URL_TUNNEL=https://api.peipaoenglish.cn/api/v1
```

发布构建补充：

- `frontend/.env.production` 已预置线上地址 `https://api.peipaoenglish.cn/api/v1`
- 执行 `npm run build:mp-weixin` 时，生产构建默认会读取该文件
- 因此发布版小程序不需要再手工把 API 地址改回线上域名

开发 / 发布目录区分：

- `frontend/dist/dev/mp-weixin`：开发包，来自 `npm run dev:mp-weixin`，读取 `.env.local`
- `frontend/dist/build/mp-weixin`：发布包，来自 `npm run build:mp-weixin`，读取 `.env.production`
- 如果微信开发者工具误打开了 `dist/build/mp-weixin`，即使本地已经切到 `lan`，请求也仍会打到线上域名

3 tab 版本补充：

- 维护分支：`codex/three-tab-training-hub`
- 微信开发者工具本地联调仍优先打开 `frontend/dist/dev/mp-weixin`
- 当前 3 tab 信息架构为 `首页 / 误解本 / 英教系统`
- `英教系统` tab 页内展示全称：`高级智能英语教练系统`

### 运行时覆盖

前端还支持运行时覆盖地址，存储键为：

- `apiBaseUrlOverride`

对应能力在：

- `setApiBaseUrlOverride(url)`
- `clearApiBaseUrlOverride()`
- `getApiRuntimeSummary()`

便于后续做调试开关页，而不需要改业务代码。

默认情况下该能力是关闭的：

- `VITE_API_ALLOW_OVERRIDE=false`
- 如果小程序本地缓存里残留了旧的 `apiBaseUrlOverride`，启动时会自动清掉
- 只有显式把 `VITE_API_ALLOW_OVERRIDE=true` 打开时，运行时覆盖地址才会生效

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

### 方案 B：公网 HTTPS 模式

适用场景：手机不在同一局域网、需要远程演示，或准备直接接近线上环境调试。

要求：

1. 使用 `ngrok` / `frp` / `cpolar` 等工具暴露本机 8080，或直接使用线上域名
2. 前端 `VITE_API_MODE=tunnel`
3. `VITE_API_BASE_URL_TUNNEL` 指向公网 HTTPS 地址

优点：

- 不依赖同一网络
- 更接近线上访问链路

当前线上 API：

- `https://api.peipaoenglish.cn/api/v1`

前端登录态恢复补充：

- 首页在拉取 `/users/{id}/dashboard` 时，如果后端返回 `用户不存在`
- 前端会自动清理本地会话：
  - `kaoyan_token`
  - `kaoyan_user_id`
  - `kaoyan_onboarding_done`
- 然后强制重新执行一次 `wx-login`
- 这样在切换本地库 / 服务器库后，不会长期卡死在旧 `userId`

微信公众平台需由你手工配置：

- 业务域名 / request 合法域名：`https://api.peipaoenglish.cn`
- 如果后续接文件上传下载，再补：
  - uploadFile 合法域名：`https://api.peipaoenglish.cn`
  - downloadFile 合法域名：`https://api.peipaoenglish.cn`

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

## 7. 当前训练闭环补充

### 首页刷新策略

当前首页不仅在首次进入时拉取 dashboard，也会在以下时机自动刷新：

- 页面 `onShow`
- 打卡成功后

这意味着：

- 从单词页或阅读页返回首页后，`继续上次训练 / 已掌握单词 / 待回炉词 / 累计打卡` 会重新按后端真实数据刷新
- 首页现在是训练状态汇总页，而不只是首次加载一次的静态展示页

### 阅读 MVP 范围

当前阅读模块是 MVP 首版，范围刻意压缩为一条可验证闭环：

- 后端 `GET /api/v1/reading/article?userId=`
- 后端 `POST /api/v1/reading/coach`
- 后端 `POST /api/v1/reading/translate`
- 后端 `GET /api/v1/mistakes?userId=&type=word|sentence`
- 后端 `POST /api/v1/mistakes`
- 前端阅读页流程：
  - 先按自然段分页展示文章
  - 再展示一道更接近真题形态的选择题（题干 + 4 个选项）
  - 用户先说思路，而不是直接点答案提交
  - AI 连续追问 1-2 轮
  - 最后展示参考答案与解析

当前已不再是纯内存 mock：

- SpringBoot 启动时会自动补齐以下表：
  - `reading_article`
  - `reading_question`
  - `reading_record`
  - `reading_translation_log`
  - `mistake_item`
- 如果 `reading_article` 为空，启动时会自动插入一篇示例文章和两道示例题

这样新机器或新库只要基础 MySQL 已可用，阅读模块与误解本 MVP 就不会因为“没表 / 没数据”直接空掉。

当前阅读输入方式：

- 文字输入：最大 1000 字
- 语音录入：单次最多 30 秒，结束后自动调用 `/speech/recognize` 转文字
- 语音转写结果会自动回填到输入框，用户仍可手动补充或修改

当前阅读辅助方式：

- `短按单词 = 选中单词`
- `长按单词 = 选中该词所在整句`
- 段落切换支持：
  - 点击 `上一段 / 下一段`
  - 左右手滑切换段落
- 阅读正文区当前不再用 `swiper` 承载正文，而是采用普通自适应高度容器 + 手势切段
- 这样可以避免首段第一次进入时因为 `swiper` 首屏布局时机导致正文被裁切
- 段落切换时带轻量翻页动效：
  - 下一段：轻微右入左停 + 淡入
  - 上一段：轻微左入右停 + 淡入
- 每次进入阅读页面都会生成新的 `readingSessionId`
- 当前 `readingSessionId` 下：
  - 单词翻译最多 `5` 次
  - 短句翻译最多 `3` 次
- 单词或短句对应次数耗尽后，前端翻译按钮会置灰，并提示对应文案：
  - `每篇最多5次翻译机会哦～`
  - `每篇短句翻译最多3次哦～`
- `加入误解本` 不受这 5 次翻译机会限制；如果当下还没有翻译文本，后端会补做一次翻译后再入库
- 加入成功后，前端会显示绿色成功提示条，文案类似：
  - `单词已经加入误解本`
  - `短句已经加入误解本`
- 误解本内翻卡查看释义不受次数限制
- 误解本卡片背面的 `重新解释` 会再次调用 AI，但做了后端防频点；频繁点击会返回：
  - `42901 / 请不要频繁点击，稍后再试`
- 误解本列表支持左滑操作：
  - 左滑后出现 `done`
  - 点击后会把该条目标记为已完成，并从当前误解本列表移除
  - `done` 只在左滑状态可见，翻卡时不会露出

当前单词训练已开始接入误解本：

- 正常仍按一轮 `20` 词组织
- 若当前用户存在误解本单词，系统会额外随机混入最多 `10` 个误解本单词
- 目的是在完成 unit 主线后，继续穿插巩固真实做题里暴露出来的薄弱词

当前翻译接口不是“统一查词库”，而是三层策略：

1. 高频功能词本地词典
   - 例如 `and / or / but / if / you / we / they / the / a / to / of / in`
   - 这类词直接本地返回常见中文义项，不消耗 LLM
2. 项目词库 `word` 表
   - 如果词库中存在该词，直接返回词库释义，并做简化输出
3. LLM 兜底
   - 仅在高频词词典和项目词库都未命中时调用
   - 句子翻译始终直接走 LLM

当前所有 token 消耗型接口都已增加后端频控，当前覆盖：

- `POST /api/v1/users/onboarding`
- `POST /api/v1/reading/coach`
- `POST /api/v1/reading/translate` 中实际走到 LLM 的请求
- `POST /api/v1/mistakes/{id}/re-explain`

前端请求封装现在不再只按 HTTP 状态码判断成功，而是也会识别业务返回里的 `code` 字段；因此像翻译次数超限、频繁点击等限制会直接给用户正确提示。

这样做的目的不是把阅读退化成普通刷题，而是保留真题选择题结构，同时强化“先表达判断路径”的 AI 教练式训练方式。

---

## 8. 当前运行结论（2026-06-22）

截至 2026-06-22，项目已在当前 macOS 机器上验证通过：

- MySQL 可启动并可连接
- `kaoyan_peipao` 已建库
- `user` / `word` / `word_progress` 已建表
- `word` 表已导入 6,547 条词库数据
- SpringBoot 后端可正常启动
- 学习画像后端已真实调用 DeepSeek 生成方案
- 微信登录接口已可编译并接入首页主链路
- 微信小程序前端可正常构建
- `npm run type-check` 通过
- `lan` 模式真机联调已验证通过
- 当前默认先走 `lan` 模式做同网段真机联调；如果只想在本机开发，可临时切回 `local`
