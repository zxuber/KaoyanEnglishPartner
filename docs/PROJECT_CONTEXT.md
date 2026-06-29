# PROJECT_CONTEXT.md - 项目状态看板

> 最后更新：2026-06-29（误解本资产库去重约束 + DONE 聚合模块开发）
> 当前阶段：第一期 MVP 开发中（`codex/three-tab-training-hub` 分支用于维护 3 tab 版本，不主动合入 master）

---

## 一、项目速览

| 项目 | 说明 |
|------|------|
| 项目名称 | 考研英语陪跑 |
| 项目类型 | 微信小程序（AI 教练式刷题） |
| 目标用户 | 有明确考研英语提分目标、需要被带着练输出的考研考生 |
| 当前版本 | v0.1.0-dev |
| 仓库地址 | 当前仓库为 Git 管理，多机协作，运行环境不再绑定固定本地路径 |

### 协作约定

- `docs/assets/` 当前用于存放 App 临时图标等临时资源，默认不纳入常规提交与推送
- 后续 Git 提交说明统一使用中文
- 3 tab 版本代码维护在 `codex/three-tab-training-hub` 分支；`master` 保持现有稳定线，未经确认不合并该分支

## 二、已完成事项

### 2.1 文档产出

| 文档 | 路径 | 状态 |
|------|------|:--:|
| 产品需求文档 (PRD v1.2) | `docs/PRD.md` | ✅ 完成 |
| 项目宪法 (AGENTS.md) | `AGENTS.md` | ✅ 完成 |
| 项目状态看板 | `docs/PROJECT_CONTEXT.md` | ✅ 完成 |
| 开发计划 | `docs/DEVELOPMENT_PLAN.md` | ✅ 完成 |
| 子智能体定义 | `subagents/*/AGENTS.md` | ✅ 完成 |
| API 密钥记录 | `docs/API_KEYS.md` | ✅ 完成 |
| 踩坑记录 | `docs/TROUBLESHOOTING.md` | ✅ 完成 |
| 服务器发布手册 | `docs/DEPLOYMENT_RUNBOOK.md` | ✅ 完成 |

### 2.2 技术决策

| 决策项 | 结论 | 日期 |
|--------|------|------|
| 前端框架 | uni-app (Vue 3 + TypeScript) | 2026-06-20 |
| 后端框架 | Java SpringBoot 3.x + MyBatis-Plus | 2026-06-20 |
| 微信 SDK | WxJava | 2026-06-20 |
| 数据库 | MySQL 8.0 | 2026-06-20 |
| 缓存 | ~~Redis~~ → Spring Simple Cache（本地无 Redis） | 2026-06-21 |
| LLM | DeepSeek-V3 (OpenAI 兼容 API) | 2026-06-20 |
| **语音识别** | **百度 ASR**（免费 5万次/天）+ ffmpeg 自动转码 | 2026-06-21 |
| 风险策略 | 版权/一致性 → 用户已确认接受 | 2026-06-20 |
| **单词判定引擎** | **同义词词林（cilin.txt, 17,752 组）+ 四级本地检查** | 2026-06-21 |

### 2.3 环境准备

| 项目 | 状态 |
|------|:--:|
| 微信小程序 AppID 注册 (`wx16cae95889539ba6`) | ✅ 完成 |
| 微信开发者工具安装 | ✅ 完成 |
| uni-app 脚手架初始化 | ✅ 完成 |
| SpringBoot 项目初始化 | ✅ 完成 |
| MySQL 数据库建库 (`kaoyan_peipao`) | ✅ 完成 |
| 词库数据导入 (6,547 词) | ✅ 完成 |
| 词库 Unit 重分配 (必考词 26 单元 + 基础词 29 单元) | ✅ 完成 |
| DeepSeek API Key | ✅ 完成 |
| 百度 ASR API Key（免费额度） | ✅ 完成 |
| 跨平台配置结构（Win/Mac） | ✅ 完成 |
| 前端 API 地址三模式（local/lan/tunnel） | ✅ 完成 |
| DeepSeek Key 改为运行时环境变量注入 | ✅ 完成 |
| macOS 本地环境验证（MySQL/后端/前端构建） | ✅ 完成 |
| 局域网 LAN 真机联调验证 | ✅ 完成 |
| 阿里云 Ubuntu 服务器初始化 | ✅ 完成 |
| 生产 MySQL 建库建表与词库导入（6547 词） | ✅ 完成 |
| `api.peipaoenglish.cn -> Nginx -> 127.0.0.1:8080` | ✅ 完成 |
| 前端生产环境 API 地址配置 | ✅ 完成 |
| 首页旧登录态自动恢复（用户不存在时自动重登） | ✅ 完成 |
| 本地联调环境回切（当前默认 LAN） | ✅ 完成 |

| ffmpeg | ✅ 完成 | 音频转码（WebM→WAV），路径配置 `ffmpeg-path: ffmpeg`（兼容 Win/Mac/Linux）
| Git 仓库初始化 | ✅ 完成 | 首次提交 6c3be70，含 .gitignore

### 2.4 模块完成情况

| 编号 | 模块 | 状态 | 备注 |
|------|------|:--:|------|
| M1 | 学习画像 | 🔄 已重构一版 | 当前 8 步结构先保持稳定，短期不再扩题，作为首次分流入口使用 |
| M2 | 方案生成 | ✅ 首版已接通 | 学习画像提交后端会真实调用 DeepSeek 生成方案，并持久化到 `user.plan_json` |
| **M3** | **单词闪卡** | **🔄 开发中** | 语音+暂停+判题链路稳定；日志清晰；前端判对即时反馈 |
| M4 | 今日训练首页骨架 | ✅ 首版已落地 | 首页最大主卡固定为今日单词训练，继续上次训练降为次级卡；今日推荐文案合并进专项入口；返回首页会自动刷新 |
| M5 | 用户认证 | ✅ 首版已落地 | 已接入 `uni.login + wx-login + JWT`，替换临时本地 session 主链路 |
| M6 | 阅读陪练 MVP | ✅ 已增强 | 固定文章 + 选择题选项 + 分段阅读 + 文字/语音双输入 + AI 连续追问 + 最后给答案解析 |
| M8-lite | 误解本资产库 | ✅ 分支首版 | `单词/短句` 仍走用户误解本；`写作表达/固定搭配/易混词` 改为全局资产库 + 用户 done 进度 |

## 三、M3 单词闪卡 - 当前状态

### 已实现功能
- ✅ 开始屏幕（统计展示 + 进入按钮）
- ✅ 单词卡片 + 倒计时进度条（8秒，绿→黄→红）
- ✅ **语音录入**：按住录音 → 松手上传 → 百度 ASR 识别 → 自动提交判题
- ✅ **暂停功能**：顶栏 ⏸ 按钮，冻结倒计时，遮罩提示
- ✅ "不认识"按钮 + 超时自动揭示释义
- ✅ 每轮 20 词 + 结果页统计
- ✅ 后端 `/words/check` 判题接口 + `/words/new` 取词接口
- ✅ 测试词库降级（后端不可用时自动使用内置 20 词）

### 架构链路
```
按住 Mic → RecorderManager 录音(WAV) → uni.uploadFile
  → POST /api/v1/speech/recognize (SpringBoot)
  → Java SpeechService → Python stt.py → 百度 ASR API
  → 返回中文文本 → 自动提交 /words/check 判题 → 下一词
```

### 单词判定引擎
- 四级本地检查：直接包含 → 关键词匹配 → **近义词映射（同义词词林）** → 编辑距离
- 同义词词林 (`cilin.txt`)：从 6,547 条词库释义自动归纳，17,752 组同义词，覆盖名词/动词/形容词
- 加载方式：`@PostConstruct` 启动时从 classpath 加载到 `Map<String, Set<String>>`，查询 O(1)
- LLM fallback 条件：仅当本地四级全挂 + 零字重叠时触发（详见 PRD §3.2.5）

### 已知限制
- 语音识别需要真机（模拟器 RecorderManager 不可用）
- 百度 ASR 对纯人声识别效果好，环境噪音会影响准确率
- 后端需保持运行（`java -jar peipao-0.0.1-SNAPSHOT.jar`）
- 真机模式下不能使用 `localhost`，需切换到 `lan` 或 `tunnel` 模式
- 新机器如未设置 `DEEPSEEK_API_KEY`，LLM 相关功能不可用

### 当前已验证环境（2026-06-22）

- macOS 本机已完成 MySQL 重建、建库、建表、词库导入
- `word` 表已导入 6,547 条数据
- 后端已在本机成功启动并验证 `/api/v1/words/new`、`/api/v1/words/stats`
- 前端已成功构建微信小程序产物
- 局域网联调模式已验证通过，当前开发机 LAN 地址为 `172.30.17.43`
- 后端新增接口已通过 Java 17 下 `mvn -DskipTests compile`
- 前端已通过 `npm run type-check`
- 微信小程序产物已重新构建通过

## 四、技术栈详情

| 层 | 技术 | 版本 |
|-----|------|------|
| 前端 | uni-app (Vue 3 + Pinia + uView Plus) | 3.0 |
| 后端 | SpringBoot + MyBatis-Plus + WxJava | 3.2.0 |
| 数据库 | MySQL | 8.0 |
| LLM | DeepSeek-V3 | - |
| 语音识别 | 百度 ASR (Python urllib 调用) | - |
| API 文档 | Knife4j (Swagger) | 4.4.0 |

## 五、当前产品方向

- 产品定位从“陪跑/背词工具”进一步收敛为 `AI 教练式刷题平台`
- 差异化不在展示题目，而在于逼用户主动输出，尤其是语音回答、思路表达和追问式训练
- 学习画像用于首次分流，不和正式首页抢入口
- 正式首页定位为 `今日训练首页`
- 首页首屏优先级明确为：
  1. `今日单词训练`：固定最大主卡，作为每日最高频、最稳定的主入口
  2. `继续上次训练`：放在主卡下方的次级卡，承接 recent training，不抢首页主入口
  3. `专项训练`：吸收原 `今日推荐` 的推荐文案，匹配画像/任务的专项显示 `今日优先` 标签
  4. `工具与检测`
  5. `最近复盘`
- `专项入口` 放在首页内，但应出现在学习画像之后，而不是之前
- 老用户回访时，默认直达首页并优先续练，不重新做画像
- 3 tab 信息架构分支采用底部导航：`首页 / 误解本 / 英教系统`
- `英教系统` tab 页内展示全称：`高级智能英语教练系统`
- 首页专项入口中，`阅读/翻译/完形填空/新题型` 直接进入对应功能页；只有 `写作` 进入二级页，二级页分 `大作文/小作文`
- `新题型` 采用二级分流页，覆盖英语一 `排序题/7选五/小标题`，英语二 `多项对应/小标题`

## 六、当前实现状态

- `学习画像 -> LLM 方案生成 -> 首页承接` 已形成首条闭环
- 首页已不再是简单占位页，而是 `今日训练首页`
- 当前首页已接入：
  - `今日单词训练` 固定主卡
  - `继续上次训练`
  - `专项入口`，并合并 `今日推荐` 的 subtitle / reason / priority
  - `最近复盘`
- 首页数据闭环已增强：
  - 打卡成功后会立即重拉 dashboard
  - 从训练页返回首页时会自动刷新 dashboard
  - 单词训练会持续回写 `recent-training`，完成后首页会展示“本轮已完成”的最新状态
- 单词模块会把最近训练状态回写后端，供首页继续训练卡片使用
- 阅读模块已从纯 mock 升级到真实后端结构：
  - 自动建表：
    - `reading_article`
    - `reading_question`
    - `reading_record`
    - `reading_translation_log`
    - `mistake_item`
  - 启动时会自动补一篇示例阅读和两道示例题，避免新环境没有阅读数据
  - 当前阅读接口：
    - `GET /api/v1/reading/article?userId=`
    - `POST /api/v1/reading/coach`
    - `POST /api/v1/reading/translate`
  - 当前误解本接口：
    - `GET /api/v1/mistakes?userId=&type=word|sentence`
    - `POST /api/v1/mistakes`
  - 当前题目已改成更接近考研阅读的选择题形态，前端会展示 `题干 + A/B/C/D 选项`
  - 但产品交互仍不是让用户只点答案，而是要求用户先说自己的判断路径，再由 AI 追问
  - 阅读页顶部已去掉额外说明卡片，避免视觉突兀；说明文案回收到主标题下方，篇目信息并入文章区域
  - 阅读输入现已支持：
    - 文字输入，最大 1000 字
    - 语音录入，单次最多 30 秒，结束后自动转文字，可再手动修改
  - 阅读文章展示现已改成按自然段分页阅读，避免整篇长文一次性压给用户
  - 阅读段落切换现已支持两种方式：
    - 点击 `上一段 / 下一段`
    - 左右手滑切换段落
  - 阅读正文区现已采用普通自适应高度容器，而不是 `swiper` 承载正文，避免首段首次进入时被裁切
  - 段落切换时会带轻量翻页动效：
    - 下一段：轻微右入左停 + 淡入
    - 上一段：轻微左入右停 + 淡入
  - 阅读辅助现已支持：
    - `短按单词 = 选中单词`
    - `长按单词 = 选中该词所在整句`
    - 每次进入阅读页面都会生成新的阅读会话
    - 当前会话内：
      - 单词翻译最多 `5` 次
      - 短句翻译最多 `3` 次
    - 选中后可 `翻译` 或 `加入误解本`
    - 加入成功后，阅读页会显示绿色成功提示条，而不是默认黑色 toast
    - 单词翻译优先走本地高频词词典和项目词库，命中时不消耗 LLM
    - 短句翻译直接走 LLM，因此单独限制为 `3` 次
  - 误解本 MVP 现已支持：
    - `单词 / 短句 / 写作表达 / 固定搭配 / 易混词 / DONE` 六个模块切换
    - 卡片翻面查看中文释义 / 译文
    - 卡片背面可点击 `重新解释`，重新走一次 AI 释义
    - 左滑后显示 `done` 按钮，可把单词 / 短句 / 写作表达 / 固定搭配 / 易混词移出当前列表
    - `done` 仅在左滑时显示，翻卡时不会露出
    - `DONE` 模块只收录误解本中已点击 `done` 的单词，作为“已掌握词库”回看；不收录短句、写作表达、固定搭配、易混词
  - 3 tab 分支新增误解本资产库：
    - 表：`mistake_asset_library`
    - 用户进度表：`mistake_asset_progress`
    - 初始化脚本：
      - `backend/docs/database/migrations/005_create_mistake_asset_tables.sql`
      - `backend/docs/database/migrations/006_seed_mistake_assets.sql`
    - 原始数据：`写作表达 / 固定搭配 / 易混词` 各 100 条，共 300 条
    - 数据约束：同一 `category` 下 `source_text` 必须唯一，不能用不同 `subcategory` 复制同一条内容
    - 2026-06-29 已修复本地种子数据重复问题：`writing / phrase / confusion` 均为 `100` 条，且 `COUNT(DISTINCT source_text)=100`
    - 接口：
      - `GET /api/v1/mistake-assets?userId=&category=writing|phrase|confusion`
      - `DELETE /api/v1/mistake-assets/{id}?userId=`
      - `GET /api/v1/mistakes/done?userId=`
    - 资产库 `done` 只影响当前用户，不删除全局原始数据
  - 单词训练现已开始接入误解本闭环：
    - 常规一轮 20 词仍保留
    - 当用户存在误解本单词时，会额外随机混入最多 `10` 个误解本单词，帮助巩固
- 所有会消耗 token 的接口现已加后端防频点：
    - 学习画像生成专属方案
    - 阅读 AI 教练追问
    - 阅读里需要 LLM 的翻译
    - 误解本 `重新解释`
  - 前端请求层现已识别业务 `code != 0` 的返回，不再只按 HTTP 200 误判为成功
- 微信登录已采用正式接口：
  - 前端 `uni.login`
  - 后端 `/api/v1/auth/wx-login`
  - 返回 `JWT + userId + onboardingDone`
- 学习画像提交时会优先写回当前登录用户，不再默认每次新建匿名用户
- 首页拉取 dashboard 时，如果发现本地旧 `userId` 在当前环境不存在，前端会自动清 session 并重新执行 `wx-login`
- 后端日志体系已切到 `Log4j2`：
  - 配置文件：`backend/src/main/resources/log4j2-spring.xml`
  - 默认日志目录：`logs`
  - 主日志文件：`logs/info.log`
  - 按天滚动：`logs/info-YYYY-MM-DD.log.gz`
  - 可用环境变量 `APP_LOG_DIR` 覆盖日志目录
  - 请求日志会带 `X-Request-Id` / MDC `requestId`，便于后续接入日志平台和串联一次请求的业务链路

## 七、近期计划

**下一步动作**（按顺序）：
1. 等待 `peipaoenglish.cn` 域名备案结果
2. 期间继续走本地 `lan` 联调，暂停服务器链路依赖
3. 继续把阅读 MVP 从固定文章扩展到可配置题库
4. 备案通过后，在微信公众平台补齐合法域名并恢复线上验证

## 八、工具启动命令

```bash
# macOS: 切到 Java 17 + Homebrew 工具链
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"
export APP_STT_PYTHON_COMMAND=python3

# 构建并启动后端
cd backend
mvn spring-boot:run

# 前端开发：本机 / 局域网 / 公网 HTTPS 三模式由环境文件控制
cd frontend
npm run build:mp-weixin
# 产出在 dist/build/mp-weixin/，用微信开发者工具导入
```

生产发布时默认读取：

- `frontend/.env.production`
- 当前线上 API：`https://api.peipaoenglish.cn/api/v1`

当前开发默认读取：

- `frontend/.env.local`
- 当前 LAN 默认 API：`http://172.30.17.43:8080/api/v1`

Windows 启动方式、环境变量入口、真机联调三模式详见：

- `docs/SETUP_AND_RUNTIME.md`
- `docs/DEPLOYMENT_RUNBOOK.md`

---

> 本文档由 `manager` 维护，每次任务完成后更新。
