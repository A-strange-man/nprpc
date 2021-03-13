package consumer;

import com.google.protobuf.*;
import controller.NRpcController;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import rpcMeta.RpcMetaProto;
import util.ZKClinetUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

public class RpcConsumer implements RpcChannel {

    private static final String ZK_SERVER = "zookeeper";
    private String zkServer;

    public RpcConsumer(String configFile) {
        Properties pro = new Properties();
        try {
            pro.load(RpcConsumer.class.getClassLoader().getResourceAsStream(configFile));
            this.zkServer = pro.getProperty(ZK_SERVER);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stub代理对象, 需要接收一个实现了RpcChannel的对象，
     * 用stub调用任意rpc方法的时候，都调用当前这个RpcChannel的callMethod方法
     * @param method
     * @param controller
     * @param request
     * @param responsePrototype
     * @param done
     */
    @Override
    public void callMethod(Descriptors.MethodDescriptor method, RpcController controller,
                           Message request, Message responsePrototype, RpcCallback<Message> done) {
        /**
         * 打包参数，递交网络发送
         * rpc调用参数格式：header_size + serviceName + methodName + args
         */
        // 根据方法获取到对应的服务对象
        Descriptors.ServiceDescriptor serviceDescriptor = method.getService();
        // 获取服务对象的名字
        String serviceName = serviceDescriptor.getName();
        // 获取方法名
        String methodName = method.getName();

        // 在zookeeper上查询serviceName 和 method 在哪个主机上（ip + port）
        String ip = "";
        int port = 0;
        ZKClinetUtils zk = new ZKClinetUtils(zkServer);
        String path = "/" + serviceName + "/" + methodName;
        String hostStr = zk.readData(path);
        zk.close();

        if (hostStr == null) {
            controller.setFailed("read path: " + path + " data from zk is invalid.");
            done.run(responsePrototype);
            return;
        } else {
            String[] host = hostStr.split(":");
            ip = host[0];
            port = Integer.parseInt(host[1]);
        }

        // 序列化头部信息
        RpcMetaProto.RpcMeta.Builder metaBuilder = RpcMetaProto.RpcMeta.newBuilder();
        metaBuilder.setServiceName(serviceName);
        metaBuilder.setMethodName(methodName);
        byte[] metaBuf = metaBuilder.build().toByteArray();

        // 参数
        byte[] argBuf = request.toByteArray();

        // 利用netty的ByteBuf组织Rpc的参数信息
        ByteBuf buf = Unpooled.buffer(4 + metaBuf.length + argBuf.length);
        buf.writeInt(metaBuf.length);   // 先写入头部信息的长度
        buf.writeBytes(metaBuf);        // 写入头部信息 serviceName + method
        buf.writeBytes(argBuf);         // 写入参数信息

        // 待发送的数据
        byte[] sendBuf = buf.array();

        // 通过网络发送rpc调用信息，因为是客户端，不需要高并发
        Socket client = null;
        OutputStream out = null;
        InputStream in = null;

        try {
            client = new Socket();
            client.connect(new InetSocketAddress(ip, port));
            out = client.getOutputStream();
            in = client.getInputStream();

            // 发送数据
            out.write(sendBuf);
            out.flush();

            // wait等待rpc调用的响应
            ByteArrayOutputStream receiveBuf = new ByteArrayOutputStream();
            byte[] rbuf = new byte[1024];
            int size = in.read(rbuf);
            /**
             *  size可能为0
             *  rpcProvider封装response响应的时候，如果参数的成员变量都是默认值，那么rpcProvider传递
             *  给rpcServer的就是一个空数据
             */
            if (size > 0) {
                receiveBuf.write(rbuf, 0, size);
                done.run(responsePrototype.getParserForType().parseFrom(receiveBuf.toByteArray()));
            } else {
                done.run(responsePrototype.getParserForType().parseFrom(new byte[0]));
            }

        } catch (IOException e) {
            controller.setFailed("server connected error!");
            done.run(responsePrototype);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (client != null) try {
                {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
