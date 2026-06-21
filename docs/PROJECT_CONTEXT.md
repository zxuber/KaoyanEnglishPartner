# PROJECT_CONTEXT.md - 项目状态看板

> 最后更新：2026-06-21 晚（近义词引擎升级 + 词库 Unit 重分配 + PRD v1.2 单词判定三层防护架构）
> 当前阶段：第一期 MVP 开发中（M3 单词闪卡进行中）

---

## 一、项目速览

| 项目 | 说明 |
|------|------|
| 项目名称 | 考研英语陪跑 |
| 项目类型 | 微信小程序（工具型） |
| 目标用户 | 熟人用户群体（同学/朋友），英语基础薄弱的考研考生 |
| 当前版本 | v0.1.0-dev |
| 仓库地址 | 本地 `C:\Users\zxube\Documents\考研英语` |

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

| ffmpeg | ✅ 完成 | 音频转码（WebM→WAV），路径配置 `ffmpeg-path: ffmpeg`（兼容 Win/Mac/Linux）
| Git 仓库初始化 | ✅ 完成 | 首次提交 6c3be70，含 .gitignore

### 2.4 模块完成情况

| 编号 | 模块 | 状态 | 备注 |
|------|------|:--:|------|
| M1 | 入门问卷 | ⬜ | 前端页面+后端API 基础框架已搭建，待完善 |
| M2 | 方案生成 | ⬜ | 后端 Service 已写，待 LLM prompt 联调 |
| **M3** | **单词闪卡** | **🔄 开发中** | 语音+暂停+判题链路稳定；日志清晰；前端判对即时反馈 |
| M4 | 数据看板骨架 | ⬜ | 前端页面占位 |
| M5 | 用户认证 | ⬜ | 当前用本地 session ID 临时方案 |

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

## 四、技术栈详情

| 层 | 技术 | 版本 |
|-----|------|------|
| 前端 | uni-app (Vue 3 + Pinia + uView Plus) | 3.0 |
| 后端 | SpringBoot + MyBatis-Plus + WxJava | 3.2.0 |
| 数据库 | MySQL | 8.0 |
| LLM | DeepSeek-V3 | - |
| 语音识别 | 百度 ASR (Python urllib 调用) | - |
| API 文档 | Knife4j (Swagger) | 4.4.0 |

## 五、近期计划

**下一步动作**（按顺序）：
1. 真机测试 M3 单词闪卡语音识别
2. 完善 M1 入门问卷页面
3. 联调 M2 LLM 方案生成
4. 完成 M5 微信登录（替换临时 session ID）

## 六、工具启动命令

```bash
# 启动 MySQL
Start-Service -Name MySQL

# 构建并启动后端
cd backend
mvn package -DskipTests
java -jar target/peipao-0.0.1-SNAPSHOT.jar

# 构建前端
cd frontend
npm run build:mp-weixin
# 产出在 dist/build/mp-weixin/，用微信开发者工具导入
```

---

> 本文档由 `manager` 维护，每次任务完成后更新。