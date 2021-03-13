package provider;

import callback.INotifyProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import rpcMeta.RpcMetaProto;

import java.nio.ByteBuffer;

/**
 * Rpc服务器端，使用netty开发
 */
public class RpcServer {

    private INotifyProvider notifyProvider;

    public RpcServer(INotifyProvider notify) {
        this.notifyProvider = notify;
    }

    public void start(String ip, int port) {
        // 创建主事件循环，对应I/O线程，主要用于处理新用户的连接事件
        EventLoopGroup mainGroup = new NioEventLoopGroup(1);
        //创建worker线程，主要用来处理已连接用户的可读写事件
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);

        // 定义一个netty网络服务的启动辅助类
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(mainGroup, workerGroup)
                .channel(NioServerSocketChannel.class)          //底层使用java NIO Selector模型
                .option(ChannelOption.SO_BACKLOG, 1024)   // 设置TCP参数, SO_BACKLOG:TCP三次握手成功后全连接队列的长度
                .childHandler(new ChannelInitializer<SocketChannel>() {      // 注册事件回调，把业务层的代码和网络层的代码区分开来
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        /**
                         * 1. 设置数据的编码器和解码器     网络字节流 <=> 业务要处理的数据类型
                         * 2. 设置具体的处理器回调
                         */
                        channel.pipeline().addLast(new ObjectEncoder());    // 编码
                        channel.pipeline().addLast(new RpcServerChannel()); // 设置事件回调处理器
                    }
                });

        try {
            // 阻塞，开启网络服务   sync()：阻塞等待future && 出问题则抛出异常
            ChannelFuture future = bootstrap.bind(ip, port).sync();

            // 关闭网络服务
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            mainGroup.shutdownGracefully();
        }
    }

    /**
     * 继承自netty的ChannelInboundHandlerAdapter类，主要提供响应的回调操作
     */
    private class RpcServerChannel extends ChannelInboundHandlerAdapter {
        /**
         * 处理接收到的事件
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            /**
             * netty=>ByteBuf
             * request是远程发送过来的Rpc调用请求的所有参数
             */
            ByteBuf request = (ByteBuf) msg;

            // 先读取头部信息的长度
            int headerSize = request.readInt();

            // 读取头部信息 (包含的服务对象名称和服务方法名称)
            byte[] metaBuf = new byte[headerSize];
            request.readBytes(metaBuf);

            // 反序列化，生成RpcMeta
            RpcMetaProto.RpcMeta rpcMeta = RpcMetaProto.RpcMeta.parseFrom(metaBuf);
            String serviceNam = rpcMeta.getServiceName();
            String methodName = rpcMeta.getMethodName();

            // 读取rpc方法的参数
            byte[] argBuf = new byte[request.readableBytes()];
            request.readBytes(argBuf);

            // 通过接口让RpcProvider调用相应的方法
            byte[] response = notifyProvider.notify(serviceNam, methodName, argBuf);

            // 把rpc方法调用的响应response通过网络发送给rpc调用方
            ByteBuf buf = Unpooled.buffer(response.length);     // 开辟空间
            buf.writeBytes(response);
            ChannelFuture future = ctx.writeAndFlush(buf);
            // 发完响应后，服务器将连接关闭
            if (future.sync().isSuccess()) {
                ctx.close();
            }
        }

        /**
         * 连接异常处理
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
        }

    }
}
