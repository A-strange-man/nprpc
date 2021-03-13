import consumer.RpcConsumer;
import controller.NRpcController;

/**
 * 客户端测试login方法
 */
public class App {
    public static void main(String[] args) {
        /**
         * 模拟Rpc方法调用者
         */
        UserServiceProto.UserServiceRpc.Stub stub =
                UserServiceProto.UserServiceRpc.newStub(new RpcConsumer("config.properties"));

        UserServiceProto.LoginRequest.Builder login_builder = UserServiceProto.LoginRequest.newBuilder();
        login_builder.setName("zhang san");
        login_builder.setPwd("66666");

        NRpcController controller = new NRpcController();
        stub.login(controller, login_builder.build(), response -> {
            /**
             * Rpc方法调用完成后的返回值
             */
            if (controller.failed()) {
                System.out.println(controller.errorText());
                controller.reset();

            } else {
                System.out.println("receive rpc call response!");
                if (response.getErrno() == 0) {
                    // 方法调用正常
                    System.out.println(response.getResult());
                } else {
                    // 方法调用出错,打印错误信息
                    System.out.println(response.getErrinfo());
                }
            }
        });
    }

}
