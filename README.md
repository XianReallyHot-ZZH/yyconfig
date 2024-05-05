# yyconfig 配置中心
[yyconfig](https://github.com/XianReallyHot-ZZH/yyconfig)是一个持久化的配置中心中间件。

## 项目包括如下几个部分

* [yyconfig-server](./yyconfig-server)：配置中心服务端，负责配置的存储、发布、同步等。
* [yyconfig-client](./yyconfig-client)：配置中心客户端，负责配置的获取、更新等。
* [yyconfig-demo](./yyconfig-demo)：yyconfig-demo: 配置中心客户端demo。

## 当前进展

* yyconfig-demo 验证性代码开发完毕
* yyconfig-server 基础功能开发完毕
* yyconfig-client 开发中...
  1. 完成启动阶段从配置中心获取配置，对@Value和@ConfigurationProperties注解的属性进行赋值
  2. 完成运行期的配置更新，支持@ConfigurationProperties的属性变更
  3. TODO：完成运行期的配置更新，支持@Value的属性变更
  4. TODO：更改实现配置动态更新的定时轮询机制，改为时效性更高的机制

