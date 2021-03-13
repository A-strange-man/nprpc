package provider;

import callback.INotifyProvider;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Service;
import org.omg.CORBA.PUBLIC_MEMBER;
import util.ZKClinetUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Rpc方法发布的站点，只需要一个站点就可以发布当前主机上的所有Rpc方法， 需要采用单例设计模式
 */
public class RpcProvider implements INotifyProvider {
    private static final String SERVER_IP = "ip";
    private static final String SERVER_PORT = "port";
    private static final String ZK_SERVER = "zookeeper";
    private String serverIp;
    private int serverPort;
    private String zkServer;
    private ThreadLocal<byte[]> responseLocalBuf;

    /**
     * 启动Rpc站点，阻塞等待远程rpc调用请求, 提供服务
     */
    public void start() {
        // 把service和method都往zookeeper注册
        ZKClinetUtils zk = new ZKClinetUtils(this.zkServer);
        serviceMap.forEach((K, V)->{
            String path = "/" + K;
            zk.createPersistent(path, null);
            V.methodMap.forEach((a, b)->{
                String createPath = path + "/" + a;
                // 创建临时znode节点
                zk.createEphemeral(createPath, serverIp + ":" + serverPort);
                // 给节点添加监听器
                zk.addWatcher(createPath);
            });
        });

        System.out.println("rpc service start at: " + serverIp + " " + serverPort);

        // 启动Rpc网络服务
        RpcServer server = new RpcServer(this);
        server.start(serverIp, serverPort);
    }

    /**
     * 服务方法的类型信息
     */
    private class ServiceInfo {
        public ServiceInfo() {
            service = null;
            methodMap = new HashMap<>();
        }

        Service service;
        Map<String, Descriptors.MethodDescriptor> methodMap;
    }

    /**
     * 包含所有的服务对象和服务方法
     */
    private Map<String, ServiceInfo> serviceMap;

    /**
     * 注册Rpc服务方法   只要支持rpc方法的类，都实现了 com.google.protobuf.Service接口
     * @param service 服务对象的名称
     */
    public void registerRpcService(Service service) {
        Descriptors.ServiceDescriptor sd = service.getDescriptorForType();
        //获取服务对象的名称
        String serviceName = sd.getName();

        // 创建serviceInfo对象
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.service = service;

        // 获取服务对象的所有方法列表
        List<Descriptors.MethodDescriptor> methodList = sd.getMethods();
        methodList.forEach(method->{
            // 获取服务方法的名称，并将服务方法 添加进该服务对象的方法map表中
            String methodName = method.getName();
            serviceInfo.methodMap.put(methodName,method);
        });

        // 向服务对象的Map表中注册服务信息
        serviceMap.put(serviceName, serviceInfo);
    }


    /**
     * notify在多线程环境运行
     * 接收RpcServer网络模块上报的rpc调用相关信息，执行具体的rpc方法的调用
     * @param serviceName
     * @param methodName
     * @param args
     * @return 返回rpc方法调用完成的响应值
     */
    @Override
    public byte[] notify(String serviceName, String methodName, byte[] args) {
        // 获取ServiceInfo对象
        ServiceInfo serviceInfo = serviceMap.get(serviceName);
        // 获取服务对象
        Service service = serviceInfo.service;
        // 获取服务方法
        Descriptors.MethodDescriptor method = serviceInfo.methodMap.get(methodName);

        // 根据方法生成请求参数对象
        Message request = service.getRequestPrototype(method).toBuilder().build();
        //反序列化
        try {
            request = request.getParserForType().parseFrom(args);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        // 调用相应的本地方法  ThreadLocal<byte[]> responseLocalBuf  ThreadLocal可以解决线程安全问题
        service.callMethod(method, null, request, response -> {
            this.responseLocalBuf.set(response.toByteArray());
        });

        return this.responseLocalBuf.get();
    }


    /**
     * 封装RpcProvider对象创建的细节
     */
    public static class Builder {
        private static RpcProvider instance = new RpcProvider();

        /**
         * 从配置文件中读取Rpc server的 ip 和 port，给instance对象初始化数据
         * 通过builder创建一个RpcProvider对象
         * @param configFileName 配置文件
         * @return
         */
        public RpcProvider build(String configFileName) {
            Properties pro = new Properties();
            try {
                pro.load(Builder.class.getClassLoader().getResourceAsStream(configFileName));
                instance.setServerIp(pro.getProperty(SERVER_IP));
                instance.setServerPort(Integer.parseInt(pro.getProperty(SERVER_PORT)));
                instance.setZkServer(pro.getProperty(ZK_SERVER));
                return instance;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 返回一个对象建造器
     * @return
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    // RpcProvider的构造函数
    private RpcProvider() {
        this.serviceMap = new HashMap<>();
        this.responseLocalBuf = new ThreadLocal<>();
    }

    public String getServerIp() {
        return serverIp;
    }

    private void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    private void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setZkServer(String zkServer) {
        this.zkServer = zkServer;
    }
}
