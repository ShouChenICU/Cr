# Cr

![Java11](https://img.shields.io/badge/Java-11-red)
![Maven3](https://img.shields.io/badge/MAVEN-3-blue)

## 这是什么？

&emsp;&emsp;这是一个用Java开发的即时通讯内核。它封装了大量简单易用的接口，同时通过设置一些回调来与外界通信，内部实现了一个即时通讯工具的所有细节，包括网络连接和加密传输、实体数据的序列化和反序列化、聊天室和成员的管理、房间和消息的同步等，只需要给它设计一个外壳，它就可以开始工作了。

## 如何开始？
&emsp;&emsp;本项目构建出来是一个jar库文件，导入到其他项目中即可开始使用，为了更好的使用它，请在使用之前为它设置两部分内容：  
1. 实现它内部提供的数据访问接口，接口目录在`/src/main/java/icu/mmmc/cr/database/interfaces`下，通过`DaoManager`的set方法进行设置。
2. 实现内部提供的回调接口，接口目录在`/src/main/java/icu/mmmc/cr/callbacks`目录下，通过直接修改`Cr.CallBack`类的静态公共字段进行设置，聊天室的接收消息回调请用`ChatRoom`实例的set方法设置。

## 第三方依赖

- [Bson](https://bsonspec.org/)
    > 二进制序列化的数据描述与交换的通用编码格式

## 构建

&emsp;&emsp;需要Maven3、JDK11+

```
    mvn clear
    mvn package
```

## 许可证

&emsp;&emsp;本项目采用[GPL v2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)开源协议开放源代码。
