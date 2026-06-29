# DEPLOYMENT_RUNBOOK.md - 服务器更新发布手册

> 最后更新：2026-06-23
> 适用范围：`peipaoenglish.cn` / `api.peipaoenglish.cn` 当前线上环境

---

## 1. 当前线上环境

服务器信息：

- 公网 IP：`39.96.63.134`
- 系统：`Ubuntu 22.04`
- 部署目录：`/opt/kaoyan/KaoyanEnglishPartner`
- 后端目录：`/opt/kaoyan/KaoyanEnglishPartner/backend`

线上域名：

- API 域名：`https://api.peipaoenglish.cn`

关键运行组件：

- Nginx：负责 HTTPS 和反向代理
- MySQL：业务数据库
- `systemd`：托管后端服务

当前后端服务：

- 服务名：`kaoyan-backend.service`
- jar 路径：`/opt/kaoyan/KaoyanEnglishPartner/backend/deploy/peipao.jar`
- 环境文件：`/opt/kaoyan/KaoyanEnglishPartner/backend/.env.server`

---

## 2. 发布前原则

发布前默认先在本地完成：

1. 代码修改
2. 本地验证
3. 提交 Git
4. 推送到 GitHub

服务器不直接做业务代码开发，服务器只负责：

1. `git pull`
2. `mvn package`
3. 重启服务
4. 验证接口

---

## 3. 标准发布流程

### 3.1 登录服务器

```bash
ssh root@39.96.63.134
```

### 3.2 进入项目目录并拉取最新代码

```bash
cd /opt/kaoyan/KaoyanEnglishPartner
git pull origin master
```

如果你后面改成别的分支发布，把 `master` 替换掉。

### 3.3 进入后端目录并重新打包

```bash
cd /opt/kaoyan/KaoyanEnglishPartner/backend
mvn -DskipTests package
```

打包成功后会生成：

- `target/peipao-0.0.1-SNAPSHOT.jar`

### 3.4 覆盖线上运行 jar

```bash
cp target/peipao-0.0.1-SNAPSHOT.jar deploy/peipao.jar
```

### 3.5 重启后端服务

```bash
systemctl restart kaoyan-backend.service
```

### 3.6 查看服务状态

```bash
systemctl status kaoyan-backend.service
```

期望看到：

- `Active: active (running)`

### 3.7 验证线上接口

```bash
curl https://api.peipaoenglish.cn/api/v1/words/stats?userId=1
```

如果返回类似：

```json
{"code":0,"message":"success",...}
```

就说明这一版已发布成功。

---

## 4. 一键版命令顺序

如果你已经确认本地代码没问题，服务器可以按下面顺序直接发版：

```bash
ssh root@39.96.63.134
cd /opt/kaoyan/KaoyanEnglishPartner
git pull origin master
cd backend
mvn -DskipTests package
cp target/peipao-0.0.1-SNAPSHOT.jar deploy/peipao.jar
systemctl restart kaoyan-backend.service
systemctl status kaoyan-backend.service
curl https://api.peipaoenglish.cn/api/v1/words/stats?userId=1
```

---

## 5. 常用排查命令

### 查看后端状态

```bash
systemctl status kaoyan-backend.service
```

### 查看后端最近日志

```bash
journalctl -u kaoyan-backend.service -n 100 --no-pager
```

### 持续跟日志

```bash
journalctl -u kaoyan-backend.service -f
```

### 查看后端业务日志文件

后端已使用 `Log4j2` 输出业务日志：

- 默认目录：后端进程工作目录下的 `logs`
- 主文件：`logs/info.log`
- 按天滚动：`logs/info-YYYY-MM-DD.log.gz`

如果 systemd 中配置了 `APP_LOG_DIR=/var/log/kaoyan-peipao`，则日志位置为：

```bash
tail -n 200 /var/log/kaoyan-peipao/info.log
tail -f /var/log/kaoyan-peipao/info.log
ls -lh /var/log/kaoyan-peipao
```

每条请求会带 `X-Request-Id`，同一次请求的请求日志、业务日志、异常日志可以通过同一个 requestId 串起来排查。

### 查看 8080 是否监听

```bash
ss -lntp | grep 8080
```

### 测试本机回环接口

```bash
curl http://127.0.0.1:8080/api/v1/words/stats?userId=1
```

### 测试线上 HTTPS 接口

```bash
curl -I https://api.peipaoenglish.cn/api/v1/words/stats?userId=1
```

### 检查 Nginx 配置

```bash
nginx -t
```

### 重载 Nginx

```bash
systemctl reload nginx
```

---

## 6. 环境文件说明

线上环境变量不放在 Git 仓库里，放在：

- `/opt/kaoyan/KaoyanEnglishPartner/backend/.env.server`

里面包含：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DEEPSEEK_API_KEY`
- `WX_MINIAPP_APPID`
- `WX_MINIAPP_SECRET`
- `APP_STT_PYTHON_COMMAND`
- `APP_STT_FFMPEG_PATH`

注意：

- 不要把真实生产 key 回写进 `application.yml`
- 不要把 `.env.server` 提交回 GitHub

---

## 7. 数据库相关操作

当前数据库名：

- `kaoyan_peipao`

如果只是普通代码更新，通常不需要动数据库。

如果后面有 SQL 变更，建议流程改成：

1. 先备份数据库
2. 执行新增 SQL
3. 再发版后端

当前词库文件来源：

- `backend/src/main/resources/words.json`

当前线上已导入：

- `6547` 条单词

---

## 8. 回滚方法

如果某次发版后接口异常，先不要慌，按这个顺序处理。

### 方案 A：回滚代码版本

先查看提交记录：

```bash
cd /opt/kaoyan/KaoyanEnglishPartner
git log --oneline -n 10
```

切到上一个稳定提交：

```bash
git checkout <稳定提交哈希>
cd backend
mvn -DskipTests package
cp target/peipao-0.0.1-SNAPSHOT.jar deploy/peipao.jar
systemctl restart kaoyan-backend.service
```

注意：

- 这是服务器临时回滚方式
- 回滚后建议你本地也同步整理 Git 分支状态

### 方案 B：先恢复服务

如果只是新包有问题，也可以先把上一次的 jar 备份保留下来，后续演进成：

- `deploy/peipao-时间戳.jar`
- `deploy/peipao.jar -> 当前版本`

当前还没做成自动多版本归档，所以现阶段仍以 Git 回滚为主。

---

## 9. 前端发布配合

当前前端生产环境默认 API：

- `https://api.peipaoenglish.cn/api/v1`

对应文件：

- `frontend/.env.production`

小程序后台已至少需要配置：

- `request 合法域名`：`https://api.peipaoenglish.cn`
- `uploadFile 合法域名`：`https://api.peipaoenglish.cn`

业务域名当前不需要，除非后面引入 `web-view` H5 页面。

---

## 10. 后续可继续优化

当前发布流程已经可用，但还可以继续升级：

1. 增加自动备份旧 jar
2. 增加数据库备份脚本
3. 增加一键发布 shell 脚本
4. 增加 CI/CD 自动发布
5. 增加健康检查接口与发布后自动验收

当前阶段先保持手工发布，最稳妥。
