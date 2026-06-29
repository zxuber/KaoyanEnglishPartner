# 考研英语陪跑官网首页

这个目录用于维护 `www.peipaoenglish.cn` 的静态官网首页，主要服务于网站备案审核和后续官网展示。

## 定位

- 备案审核阶段：展示网站名称、服务内容、域名说明和联系方式
- 正式备案通过后：底部替换为真实 ICP 备案号，并链接到 `https://beian.miit.gov.cn/`
- 不承载小程序完整业务功能，业务接口仍由 `api.peipaoenglish.cn` 提供

## 部署建议

将本目录内容同步到服务器 Nginx 静态目录，例如：

```bash
mkdir -p /var/www/peipaoenglish
cp -r website/* /var/www/peipaoenglish/
```

Nginx 中将 `www.peipaoenglish.cn` 指向该目录即可。
