# TROUBLESHOOTING.md - 踩坑记录

> 记录开发过程中遇到的实际问题、决策变更、已知限制。
> 最后更新：2026-06-21

---

## 1. 语音识别方案变更

**日期**：2026-06-21
**类型**：架构决策变更

### 问题

PRD v1.1 原定使用微信小程序原生 `wx.startRecord` + 微信语音识别 API（端上处理，零成本）。实际开发中发现：

- 微信自带的语音识别 API 对中文识别效果尚可，但对英文单词朗读的识别准确率无法满足需求
- 微信 API 的识别结果不可控，调试困难（端上黑盒）
- 需要真机才能测试，模拟器 `RecorderManager` 不可用，开发效率低

### 决策

改用 **百度 ASR 短语音识别**（免费额度 5 万次/天），通过后端中转：

```
前端: uni.getRecorderManager() 录音(WAV, 16kHz, 单声道)
  → uni.uploadFile 上传 → SpringBoot SpeechController
    → 保存临时文件 → ProcessBuilder 调 Python stt.py
      → urllib 调百度 ASR HTTP API → 返回中文文本
```

### 选择百度的理由

- 中文短语音识别效果好，免费额度充足（5 万次/天，单人日均约 200 次调用）
- API 简单，Python 标准库即可调用，无需 SDK
- 识别结果稳定可调试

### 代价

- 链路多一跳（前端→后端→Python→百度），延迟约 2-5s
- 需要本机安装 Python 3
- 依赖百度 API Key 的有效性

### 已知限制

- 模拟器 `RecorderManager` 不可用，必须真机测试语音功能
- 环境噪音会影响识别准确率
- 后端需保持运行（Java + Python 环境）

---

## 2. 词库数据获取

**日期**：2026-06-20
**类型**：实现细节

### 方案

使用 `backend/src/main/resources/words.json` 作为本地词库文件，运行时通过 `WordService` 按 Unit 分页读取。已导入 6547 词，字段包含：英文单词、中文释义、词性、所属 Unit。音标和真题例句字段预留但数据暂不完整。

---

## 3. Redis 不可用 → Simple Cache 降级

**日期**：2026-06-21
**类型**：环境限制（非设计变更）

### 问题

PRD 设计使用 Redis 做 LLM 结果缓存和会话管理，但本地开发环境未安装 Redis。

### 临时方案

`application.yml` 中排除 Redis 自动配置，改用 `spring.cache.type: simple`（ConcurrentHashMap 内存缓存）。

### 后续

Redis 仍在计划内，本地环境具备后恢复。POM 中 `spring-boot-starter-data-redis` 依赖已保留，仅需取消 `application.yml` 中的排除配置即可启用。

---

## 4. 页面路由导航问题

**日期**：2026-06-21
**类型**：实现细节

### 问题

uni-app 微信小程序中，首页跳转到各功能页（单词/阅读/作文等）后，底部 tab 栏消失，用户返回首页不方便。

### 当前方案

使用 `uni.reLaunch` 做页面间跳转，各页面顶部导航栏保留返回能力。

### 后续

考虑启用原生 tabBar 或自定义底部导航组件。

---

> 踩坑记录持续更新中。每遇到一个值得记录的问题（方案变更、环境坑、边界 case），追加一条。

---

## 5. 微信开发者工具录音格式为 WebM

**日期**：2026-06-21
**类型**：环境限制

### 问题

模拟器中 `RecorderManager` 忽略 `format: 'wav'` 参数，强制输出 WebM 格式。而百度 ASR 配置为 `format: "wav"`，导致识别结果始终为空。

验证方法：检查录音文件头部 4 字节——正确 WAV 应为 `RIFF`，模拟器产出的显示 `\x1aE\xdf\xa3`（WebM 容器）。

### 解决

`SpeechService` 增加格式检测 + ffmpeg 自动转码：
- 读文件头判断是否 WAV（RIFF 魔数）
- 非 WAV 则调用 `ffmpeg -i input -ar 16000 -ac 1 -sample_fmt s16 -f wav output.wav`
- 转码后的临时文件传给 Python stt.py，用完自动清理

### 前置条件

本机需安装 ffmpeg（仅开发机/服务器需要，用户手机不需要）。ffmpeg 路径通过 `application.yml` 中 `app.stt.ffmpeg-path` 配置。
---

## 6. Python stdout 中文被 Java 读成乱码

**日期**：2026-06-21
**类型**：编码问题

### 问题

Python 子进程 `print()` 输出中文时，Windows 默认使用 GBK 编码，Java 用 `new String(bytes)` 按系统默认编码（也是 GBK）读取时正常，但用 `StandardCharsets.UTF_8` 读取后变成 `\xEF\xBF\xBD`（Unicode 替换字符）。

stt.py 的日志输出到 stderr，识别结果到 stdout。当 `ProcessBuilder.redirectErrorStream(true)` 合并两路后，stderr 中的中文日志也受影响。

### 解决

两步同时修复：
1. Java 端：`new String(bytes, StandardCharsets.UTF_8)`——强制按 UTF-8 解码
2. Python 端：`ProcessBuilder.environment().put("PYTHONIOENCODING", "utf-8")`——强制 Python 用 UTF-8 输出

### 验证

`SpeechService` 日志新增 `raw output` 行，打印 Python 原始输出内容，可在 IDEA 控制台直接看到中文是否正确。

---

## 7. 百度 ASR 间歇性空返回

**日期**：2026-06-21
**类型**：外部服务不稳定

### 现象

同一份录音文件，多次调用百度 ASR：有时正常返回"放弃放弃。"，有时返回空 `""`，有时返回 `??????????`。非代码问题。

### 对策

不做额外处理。前端 `uploadAndRecognize` 中 `success: false` 时展示提示让用户重试。真机录音质量好，识别率会显著高于模拟器生成的 WebM 文件。

---

## 8. 词性标注污染导致判题误报

**日期**：2026-06-21
**类型**：Bug 修复

### 现象

用户回答「痴迷着魔执念」被判错误（correct=false），但标准释义为 n. 痴迷，着魔，用户答案明显正确。

### 根因

normalize() 两轮正则的执行顺序导致词性标注字母残留：

n. 痴迷，着魔
  → toLowerCase      → n. 痴迷，着魔
  → 第一轮去中文标点+空格 → n.痴迷着魔
  → 第二轮去英文标点     → n痴迷着魔   ← n. 变成 n，粘在中文前

于是 std = n痴迷着魔，ans = 痴迷着魔执念：
- std.contains(ans) → false
- ans.contains(std) → false

### 修复

新增 POS_PATTERN 常量，匹配 12 种词性标注缩写（n. vt. vi. adj. adv. prep. conj. pron. v. art. num. int. aux. abbr.），在 normalize() 第一步整体移除。 extractKeywords() 同步替换。

### 验证

WordJudgeServiceTest 新增 9 个测试用例，包含精确重现 bug 的 case。全部通过。

