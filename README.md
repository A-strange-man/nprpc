# nprpc
项目结构：

nprpcFramwork：

- 子项目nprpc：
  - RpcServer类：Rpc服务器端，使用netty网络库进行网络消息的接收和响应转发。
  - PpcProvider类：Rpc方法发布的站点，采用单例设计模式。
    - Builder构造器类：从配置文件中读取Rpc server的 ip 和 port，初始化数据。
    - start()方法：将站点上注册过的服务对象和方法向zookeeper上注册。调用RpcServer的start方法启动网络服务。
    - registerRpcService()方法：向站点注册服务对象及其对应的所有方法
  - INotifyProvider接口：提供notify方法实现RpcServer调用PpcProvider上注册的方法。利用基于接口的事件回调降低耦合。
  - ZKClinetUtils类：和zookeeper通信的工具类。
    - 构造函数：连接zookeeperServer，创建根节点。
    - close()：关闭和zk server的连接。
    - createEphemeral()：在zookeeper上创建临时性节点。
    - createPersistent()：创建永久性节点。
    - readData()：读取节点的值。
    - addWatcher()：在指定的节点添加watcher监听。
  - RpcConsumer类：重写了RpcChannel的callMethod方法。
- 子项目RpcProvider：
  - 提供了服务提供者的测试用例。
- 子项目RpcConsumer:
  - 提供了服务调用者的测试用例。