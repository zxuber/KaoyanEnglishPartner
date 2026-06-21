# CHANGELOG

> 考研英语陪跑 变更记录  
> 遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/) 格式

---

## [Unreleased]

### Added
- 同义词词林近义词引擎 (`cilin.txt`)：从 6,547 条词库释义自动归纳 17,752 组同义词，替换原有的 32 组手写硬编码枚举
- 词库 Unit 重新分配：必考词 Units 1-26（每单元 120 词），基础词 Units 27-55（每单元 120 词，末单元 70 词）
- 单词判定三层防护架构文档化（PRD §3.2.5）：本地四级检查 + LLM fallback（零字重叠触发）+ 自动熔断

### Changed
- `WordJudgeService`：近义词映射从 `static {}` 硬编码改为 `@PostConstruct` 加载 classpath `cilin.txt`
- `application.yml`：`ffmpeg-path` 从 `ffmpeg/ffmpeg.exe` 改为 `ffmpeg`（兼容 macOS/Linux）
- `init_all.sql`：补充 002 词表迁移和 003 Unit 重分配迁移的引用

### Fixed
- 修复 Unit 26（必考词最后一个单元）空缺问题——pages 151-156 的 120 词已正确归属

### Docs
- PRD 更新至 v1.2：文档化单词判定三层防护架构（§1.4 调用原则 + §3.2.5 详细策略 + §6 风险表）
- `PROJECT_CONTEXT.md`：更新技术决策记录、M3 单词判定引擎说明、词库状态
- `CHANGELOG.md`：初始化

---

## [0.1.0-dev] - 2026-06-20

### Added
- 项目脚手架：uni-app 前端 + SpringBoot 后端 + MySQL 8.0
- 微信小程序 AppID 注册与开发者工具配置
- 入门问卷 7 题基础框架（前后端）
- 单词闪卡 M3 模块：语音录入 + 倒计时 + 本地判题 + 标记词池
- 百度 ASR 语音识别集成（Python stt.py + ffmpeg 转码）
- 词库数据导入（6,547 词，JSON → MySQL）
- DeepSeek-V3 LLM API 接入
- PRD v1.0/v1.1、AGENTS.md、开发计划等全部文档
