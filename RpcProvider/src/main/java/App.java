import provider.RpcProvider;

/**
 * server端测试
 */
public class App {
    public static void main(String[] args) {
        /**
         * 启动一个可以提供rpc远程调用方法的Server
         * 1.需要有一个RpcProvider的对象，该对象由 nprpc框架提供
         * 2.向RpcProvider上面注册rpc方法   UserServiceImpl.login   UserServiceImpl.reg
         * 3.启动RpcProvider这个站点   阻塞等待远程Rpc调用请求
         */

        /**
         * 利用RpcProvider的构造器 构造一个RpcProvider对象
         */
        RpcProvider.Builder providerBuilder = RpcProvider.newBuilder();
        RpcProvider rpcProvider = providerBuilder.build("config.properties");

        /**
         * 向rpcProvider上注册服务对象
         *
         * UserServiceImpl: 服务对象的名称
         * login  reg : 服务方法的名称
         */
        rpcProvider.registerRpcService(new UserServiceImpl());

        /**
         * 启动rpc server站点，阻塞等待远程rpc调用请求
         */
        rpcProvider.start();
    }
}
