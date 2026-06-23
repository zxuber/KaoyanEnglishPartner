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

## 13. 域名备案未完成前，微信小程序线上域名链路不稳定

**日期**：2026-06-23
**类型**：上线准备 / 联调策略调整

### 现象

- 浏览器、`openssl`、Homebrew `curl` 可以访问 `https://api.peipaoenglish.cn`
- 但微信开发者工具 / 真机对该域名请求出现 `ERR_CONNECTION_RESET`
- 阿里云客服确认：微信小程序不走阿里云 App 备案，当前重点应是完成域名备案

### 当前结论

在 `peipaoenglish.cn` 域名备案正式完成前，不再把“线上域名可用于微信真机请求”作为当前开发前提。

### 当前策略

- 前端开发默认切回 `frontend/.env.local`
- `VITE_API_MODE=lan`
- 当前局域网联调地址使用 `http://172.30.17.43:8080/api/v1`
- 如只做本机模拟器开发，再临时切回 `local`

### 额外排查点

如果 `.env.local`、源码和 `dist/dev/mp-weixin` 都已经切到 `lan`，但微信开发者工具里请求仍然打到 `https://api.peipaoenglish.cn`，优先检查是否误打开了错误的产物目录：

- `frontend/dist/dev/mp-weixin`：开发包，走 `.env.local`
- `frontend/dist/build/mp-weixin`：发布包，走 `.env.production`

这次实际踩坑原因就是开发者工具一直打开的是 `dist/build/mp-weixin`，因此请求始终指向服务器域名。

### 后续恢复线上链路的条件

1. `peipaoenglish.cn` 域名备案通过
2. 微信公众平台配置合法域名
3. 再回切 `tunnel/production` 验证真实线上接口

---

## 14. 首页“重新查看学习画像与专属方案”跳空白页

**日期**：2026-06-23
**类型**：前端导航冲突

### 现象

- 首页点击“重新查看学习画像与专属方案”
- 控制台报：
  - `navigateTo:fail timeout`
  - `reLaunch:fail timeout`
- 最终落到空白页

### 根因

`/pages/onboarding/index` 原本被设计成“首次做画像”页面。

- 首页点击后先 `navigateTo('/pages/onboarding/index')`
- 但 `onboarding` 页的 `onShow` 又会在 `isOnboardingDone() === true` 时立刻 `reLaunch('/pages/home/index')`
- 两个导航动作互相打架，微信小程序最终报超时

### 解决

增加“回看方案模式”：

- 首页入口改为 `navigateTo('/pages/onboarding/index?review=1')`
- `onboarding` 页面识别 `review=1` 后：
  - 不再强制跳回首页
  - 直接请求 `GET /api/v1/users/{id}/plan`
  - 展示已保存的专属方案

### 结论

后续要区分两种场景：

- 首次进入画像：走问卷流程
- 已完成用户回看方案：走 `review=1` 模式，不能复用“首次进入就自动回首页”的逻辑

---

## 11. macOS 默认只有 python3，没有 python

**日期**：2026-06-22
**类型**：跨平台兼容

### 问题

后端原配置写死 `python-path: python`。在 macOS 上常见情况是只有 `python3` 命令，没有 `python`，导致 `SpeechService` 子进程启动失败。

### 解决

后端配置改为：

- `app.stt.python-command`

并在 `SpeechService` 中做运行时自动探测：

- Windows：优先 `python`，其次 `py`
- macOS / Linux：优先 `python3`，其次 `python`

也支持通过环境变量 `APP_STT_PYTHON_COMMAND` 显式覆盖。

---

## 12. 真机测试不能访问 localhost

**日期**：2026-06-22
**类型**：联调架构调整

### 问题

前端曾直接在多个文件里写死：

- `http://localhost:8080/api/v1`

模拟器可勉强工作，但真机访问时 `localhost` 指向的是手机自己，而不是开发机。

### 解决

前端新增统一 API 配置入口：

- `frontend/src/config/api.ts`

支持三种模式：

- `local`
- `lan`
- `tunnel`

通过 Vite 环境变量切换：

- `VITE_API_MODE`
- `VITE_API_BASE_URL_LOCAL`
- `VITE_API_BASE_URL_LAN`
- `VITE_API_BASE_URL_TUNNEL`

这样页面代码不再关心当前是本机、局域网还是真机穿透。

---

## 13. GitHub Push Protection 会拦截真实 DeepSeek Key

**日期**：2026-06-22
**类型**：仓库安全 / 发布流程

### 问题

如果把真实的 DeepSeek API Key 直接写入：

- `backend/src/main/resources/application.yml`

那么在 push 到 GitHub 时，可能会触发 Push Protection，远端直接拒绝提交。

### 解决

调整为：

- 仓库中不保留真实 Key
- `application.yml` 中只保留 `DEEPSEEK_API_KEY` 的环境变量入口
- 开发机或服务器在运行前自行注入真实环境变量

### 影响

- 代码拉下来后不会自动拥有 LLM 调用能力
- 如果没有先设置 `DEEPSEEK_API_KEY`，LLM 相关接口会不可用
- 这是预期行为，不是 bug

---

## 14. macOS Homebrew MySQL 数据目录损坏

**日期**：2026-06-22
**类型**：本地环境修复

### 现象

MySQL 安装后 `brew services list` 看起来是 started，但实际连接失败：

- `Can't connect to local MySQL server through socket '/tmp/mysql.sock'`

错误日志中反复出现：

- `ibdata1` 打不开
- InnoDB 初始化失败

### 根因

`/opt/homebrew/var/mysql` 中只残留了错误日志和不完整元数据，数据目录不可用，导致 mysqld 无法正常启动。

### 处理

- 先备份旧目录
- 重建全新的 Homebrew MySQL datadir
- 重新初始化 root 用户
- 重建 `kaoyan_peipao`
- 执行建表 SQL
- 从 `backend/src/main/resources/words.json` 导入词库

### 结果

- MySQL 恢复可用
- `word` 表成功导入 6,547 条数据
- 后端接口恢复正常

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

---

## 9. 判题日志过于精简，排查困难

**日期**：2026-06-21
**类型**：改进

### 问题

WordJudgeService 原本无日志，WordService 仅有一行汇总：
`
[Word] judge: word=obsession, meaning=n. u75f4u8ff7uff0cu7740u9b54, answer=u75f4u8ff7u7740u9b54u6267u5ff5u3002, correct=false
`
无法判断是哪个比对步骤失败、normalize 后文本长什么样、关键词提取了什么。

### 改进

WordJudgeService 增加 @Slf4j + 分步日志：
- 入口：原始释义/答案 u2192 normalize 后文本
- 关键词提取结果列表
- 每步命中时打印具体规则（contains / keyword / synonym / edit-dist）
- 全部未命中时打印 no match

现在日志形如：
`
[Judge] raw meaning=[n. u75f4u8ff7uff0cu7740u9b54] normalized=[u75f4u8ff7u7740u9b54] | raw answer=[u75f4u8ff7u7740u9b54u6267u5ff5] normalized=[u75f4u8ff7u7740u9b54u6267u5ff5]
[Judge] ans contains std -> true
`

---

## 10. WordProgress reviewInterval NPE + 前端无反馈

**日期**：2026-06-21
**类型**：Bug 修复

### 现象

语音识别正确，后端日志显示判对（correct=true），但前端报错 Cannot invoke java.lang.Integer.intValue() because getReviewInterval() is null。页面不显示对错结果，正确计数不增加。

### 根因

1. 新用户首次接触某单词时，WordProgress 记录不存在，代码 new 了一个新对象但只设了 status/mistakeCount/correctStreak，遗漏了 reviewInterval
2. 判对路径调用 getNextInterval(wp.getReviewInterval())，Integer 自动拆箱为 int 时空指针
3. 后端抛 500 → 前端 catch 块本地兜底判题，但 /words/new 返回的 WordVO 不含 meaning 字段，currentWord.meaning 为 undefined → 兜底也失败

### 修复

- WordService: 新建 WordProgress 时初始化 wp.setReviewInterval(0)
- WordController: /words/new 的 WordVO 映射增加 .meaning(w.getMeaning())
- 前端 catch 兜底: 用 currentWord.meaning 做简单 contains 判断
- 前端判对反馈: uni.showToast 弹窗提示
