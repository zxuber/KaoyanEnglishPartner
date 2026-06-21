# AGENTS.md - 项目宪法

> 项目：考研英语陪跑  
> 版本：v1.0  
> 创建日期：2026-06-20  
> 适用范围：本项目下所有智能体（Agent）及协作者

---

## 1. 项目核心定位

- **项目名称**：考研英语陪跑
- **一句话描述**：一款面向考研英语一的智能陪练微信小程序，通过 LLM 驱动的对话式交互，帮助英语基础薄弱的考生在 3 个月内从「无从下手」到「60 分可期」。
- **技术栈**：
  - 前端：uni-app (Vue 3 + TypeScript + Pinia + uView Plus 3.x)
  - 后端：Java SpringBoot 3.x + MyBatis-Plus + WxJava
  - 数据库：MySQL 8.0 + Redis
  - LLM：DeepSeek-V3（OpenAI 兼容 API）
  - 语音：微信原生 `wx.startRecord` + 微信语音识别 API
- **核心目标**：为熟人用户群体（同学/朋友）提供一个「先诊断 → 后定标 → 再分阶段执行」的考研英语陪练工具。
- **产品定位**：工具型产品，不追求 DAU，不考虑商业化上线。

## 2. 团队协作与分工

本项目采用「主智能体（Manager）+ 三个职能智能体」的四角色协作模式：

| 角色 | 标识 | 核心职责 | 禁止越界 |
|------|------|----------|----------|
| **主智能体** | `manager` | 任务拆解、分配、进度追踪、代码合并、架构决策 | 不直接写业务代码 |
| **前端智能体** | `frontend-dev` | uni-app 页面/组件开发、状态管理、UI 交互逻辑 | 不写后端 API、不操作数据库 |
| **后端智能体** | `backend-dev` | API 路由、数据库模型、业务逻辑、LLM 调用封装 | 不写前端 UI 代码 |
| **测试/文档智能体** | `qa-doc` | 单元测试、E2E 测试、API 文档、CHANGELOG 维护 | 不修改业务逻辑代码 |

**协作原则：**
- 每个智能体只在自己职责范围内工作，跨域需通过 Manager 协调
- Manager 是唯一有权合并代码到 `main` 分支的角色
- 任何两个智能体之间的接口变更，必须由 Manager 确认后才能实施

## 3. 核心工作流程

### 3.1 任务启动

```
用户提出需求
    │
    v
Manager 分析需求，拆解为子任务
    │
    v
Manager 从 main 创建 feature/{任务名} 分支
    │
    v
Manager 将子任务分配给对应智能体，每个子任务从 feature 分支再创建 work/{智能体}/{子任务名} 分支
    │
    v
各智能体在自己的 work 分支上开发
```

### 3.2 开发模式：「先文档/测试，后代码」

对于复杂度评级为「中」或「高」的功能：

1. **qa-doc** 先产出接口定义文档（API 契约）或 UI 线框描述
2. **Manager** 审核文档，确认接口契约
3. **frontend-dev / backend-dev** 基于契约并行开发
4. **qa-doc** 编写测试用例
5. 所有测试通过后，**Manager** 审查代码并合并

### 3.3 代码审查

- 任何代码合并到 `main` 分支前，必须由**非作者本人**的另一个智能体审查
- 审查要点：
  - 是否遵循本节第 4 条的工程红线
  - 是否与接口契约一致
  - 是否有对应的测试覆盖
- Manager 拥有最终合并决定权

### 3.4 任务完成

- 更新 `docs/PROJECT_CONTEXT.md` 中的模块状态
- 在 `CHANGELOG.md` 中记录变更
- Manager 确认后将 work 分支合并到 feature 分支，feature 分支合并到 main

## 4. 工程红线（不可违反的约束）

### 4.1 分支纪律

- **禁止**直接在 `main` 分支上提交代码
- **禁止**在 feature 分支上直接开发，必须在 work 分支上工作
- 分支命名规范：
  - `feature/{功能名}` —— 功能分支
  - `work/{智能体标识}/{子任务名}` —— 工作分支
  - `fix/{问题描述}` —— 紧急修复分支

### 4.2 代码质量

- **禁止**编写任何没有对应单元测试的核心业务逻辑（Service 层必须有测试）
- **禁止**硬编码 API 密钥、数据库密码、AppSecret 等敏感信息
  - 所有敏感配置使用 `application.yml` 的环境变量占位符
  - 本地开发使用 `.env.local`（已加入 `.gitignore`）
- **禁止**在前端代码中直接调用 LLM API（必须通过后端代理）
- **禁止**在循环中调用 LLM API（必须批量或缓存）

### 4.3 架构约束

- **优先**使用 uView Plus 组件库的现有组件，避免重复造轮子
- **优先**使用 MyBatis-Plus 的代码生成器生成基础 CRUD，不手写重复 SQL
- **必须**使用统一的错误码和错误响应格式（见 5.3）
- **必须**对 LLM 调用结果做超时和降级处理（超时 > 10s 返回降级响应）

### 4.4 数据安全

- **禁止**在前端缓存中存储用户手机号、openid 等敏感信息
- **禁止**将用户作文内容用于 LLM 训练或日志输出
- 所有用户输入必须在服务端进行二次验证

## 5. 代码规范

### 5.1 前端规范（uni-app / Vue 3）

- 组件文件使用 PascalCase 命名：`WordCard.vue`、`ReadingPanel.vue`
- 页面文件使用 kebab-case 命名，放在 `pages/` 下对应模块目录
- 状态管理使用 Pinia，按模块拆分 store：`wordStore`、`readingStore`、`userStore`
- API 调用统一通过 `utils/request.js` 封装的拦截器，自动注入 token
- 样式使用 uView Plus 的主题变量，不写硬编码颜色值

### 5.2 后端规范（SpringBoot）

- **API 设计**：遵循 RESTful 风格
  - URL 命名：`/api/v1/{resource}`，使用复数名词
  - GET 查询、POST 创建、PUT 全量更新、PATCH 部分更新、DELETE 删除
  - 每个模块的 API 文档必须在 `docs/api/` 下有对应的 Markdown 文件
- **分层架构**：
  ```
  Controller → Service → Mapper (DAO)
       ↓           ↓
     DTO/VO     Entity
  ```
  - Controller 只做参数校验和路由，不写业务逻辑
  - Service 层包含所有业务逻辑，必须可单元测试
  - Mapper 使用 MyBatis-Plus BaseMapper，复杂查询写在 XML 中
- **数据验证**：所有用户输入在 Controller 层用 `@Valid` 做 JSR-303 校验后，Service 层根据 PRD 中的功能规则做二次业务校验（如「目标分数必须在 40-80 之间」）

### 5.3 错误处理规范

统一错误响应格式：
```json
{
  "code": 40001,
  "message": "目标分数超出范围",
  "detail": "目标分数必须在 40-80 之间，当前值为 95",
  "timestamp": "2026-06-20T10:30:00Z"
}
```

错误码分段规则：
- `400xx`：客户端参数错误
- `401xx`：认证/授权错误
- `403xx`：权限不足
- `404xx`：资源不存在
- `500xx`：服务器内部错误
- `503xx`：LLM 调用超时/降级

### 5.4 Git 提交规范

使用 Conventional Commits：
```
feat: 新增单词闪卡语音识别功能
fix: 修复阅读精读页面滚动时计时器重置问题
docs: 更新 API 接口文档
test: 补充作文批改模块单元测试
refactor: 重构 LLM 调用路由层
```

---

> 本文件是项目最高行为准则。任何与本文件冲突的局部决策，以本文件为准。
> 修改本文件需经 Manager 审批。
