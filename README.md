
<div align=center>
<img src=doc/jvmm.png height=200/>
</div>

<div align="center">

![license](https://img.shields.io/github/license/tzfun/jvmm)
![JDK](https://img.shields.io/badge/JDK-1.8+-green)
[![maven](https://img.shields.io/maven-central/v/io.github.tzfun.jvmm/jvmm-server)](https://search.maven.org/search?q=g:io.github.tzfun.jvmm)

</div>

Jvmm是一个同时支持操作系统监控和Java虚拟机监控的工具，提供有丰富的监控功能：OS监控（内存状态、CPU负载、磁盘IO吞吐率、磁盘健康状态、网卡IO等）、JVM监控（内存、线程、GC、类加载器等），还提供生成火焰图、Java代码热更、反编译功能，支持以服务形式对外提供接口（http、哨兵等）。适合用于服务健康监控、线上调优、排查问题、性能测试等场景。

## 功能支持

* 支持操作系统监控：内存状态、CPU负载、磁盘IO及吞吐率、磁盘健康状态、网卡信息、网卡IO
* 支持Java虚拟机监控：GC信息、内存使用情况、内存池信息、类加载器、线程堆栈
* 支持生成火焰图（采样事件包括CPU、内存分配、线程栈、Java方法调用栈等）
* 支持Java代码反编译生成
* 支持Java代码热更新（可指定ClassLoader）
* 支持远程执行GC
* 支持远程执行JDK自带工具命令，包含但不限于jps、jstat、jstack、jinfo、jmap、jcmd等
* 提供客户端交互工具，支持跨进程attach和远程连接功能
* 支持三种服务模式（可同时开启多个服务）：
  * jvmm服务：独有RPC协议，需使用jvmm客户端远程连接调用，安全可靠
  * http服务：提供Http接口，不限开发语言远程调用
  * 哨兵服务：定时采集数据并上报给订阅者
* 提供多种方式使用：客户端工具跨进程attach启动、添加Java Agent启动、server独立运行启动、项目中Maven / Gradle引用server / core依赖进行自定义开发
* 支持 JDK 8+
* 支持 Linux/Mac/Windows

## 获取Jvmm

请前往[release](https://github.com/tzfun/jvmm/releases)下载[最新版的jvmm](https://github.com/tzfun/jvmm/releases/download/2.0.2/jvmm-2.0.2.zip)

## 快速使用

将Jvmm压缩包解压，可前往[releases](https://github.com/tzfun/jvmm/releases)下载，然后运行

```shell
java -jar jvmm.jar
```

或直接在你的项目中使用

```xml
<dependency>
    <groupId>io.github.tzfun.jvmm</groupId>
    <artifactId>jvmm-server</artifactId>
    <version>${jvmm-version}</version>
</dependency>
```

```java
public class JvmmServerBootDemo {
    public static void main(String[] args) {
        ServerBootstrap server = ServerBootstrap.getInstance();
        server.start(msg -> System.out.println(msg));
    }
}
```

## 使用文档

* [Jvmm工具使用文档](client/README.md)
* [两种Java Agent方式使用Jvmm](agent/README.md)
* [Core模块提供的API接口](core/README.md)
* [Server组件使用及三种服务模式说明文档](server/README.md)

## 示例

在这里提供了一些简单示例

* [API调用示例](demo/src/main/java/org/beifengtz/jvmm/demo/ApiDemo.java)
* [Server启动使用示例](demo/src/main/java/org/beifengtz/jvmm/demo/ServerBootDemo.java)
* [Jvmm连接工具使用示例](demo/src/main/java/org/beifengtz/jvmm/demo/ServerConveyDemo.java)

生成火焰图示例

![profiler.png](doc/profiler.png)

Dashboard应用示例

![dashboard](doc/dashboard.jpg)

## 联系作者

在使用过程中遇到任何问题，或者对本项目有独特的见解或建议，欢迎[提交issue](https://github.com/tzfun/jvmm/issues)

联系邮箱：[beifengtz@qq.com](mailto://beifengtz@qq.com)

## 问题解决

### 1.kernel.perf_event_paranoid权限开关
如果你在生成火焰图时提示`No access to perf events. Try --fdtransfer or --all-user option or 'sysctl kernel.perf_event_paranoid=1'`，原因是系统内核默认禁止了检测系统性能，你需要开启这个选项。

```shell
sudo systcl -w kernel.perf_event_paranoid=1
```

或者修改sysctl文件

```shell
sudo sh -c 'echo "kernel.perf_event_paranoid=1" >> /etc/sysctl.conf'
sudo sysctl -p
```

## 感谢

* profiler支持：https://github.com/jvm-profiling-tools/async-profiler
* 灵感来源以及借鉴参考：https://github.com/alibaba/arthas
