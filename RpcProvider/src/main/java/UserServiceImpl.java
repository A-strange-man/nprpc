import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * 描述：原来是本地服务，现在要发布成RPC方法
 */
public class UserServiceImpl extends UserServiceProto.UserServiceRpc {

    /**
     * 登陆业务
     * @param name
     * @param pwd
     * @return
     */
    public boolean login(String name, String pwd) {
        System.out.println("name:" + name);
        System.out.println("password:" + pwd);
        return true;
    }

    /**
     * 注册业务
     * @param name
     * @param pwd
     * @param age
     * @param sex
     * @param phone
     * @return
     */
    public boolean reg(String name, String pwd, int age,
                       UserServiceProto.RegRequest.SEX sex, String phone) {
        System.out.println(name);
        System.out.println(pwd);
        System.out.println(age);
        System.out.println(sex);
        System.out.println(phone);
        return true;
    }

    /**
     * login的 rpc 代理方法
     * @param controller  可以接收方法执行状态
     * @param request
     * @param done
     */
    @Override
    public void login(RpcController controller, UserServiceProto.LoginRequest request,
                      RpcCallback<UserServiceProto.Response> done) {
        // 1. 从request里面读取到远程rpc调用请求的参数
        String name = request.getName();
        String pwd = request.getPwd();

        // 2. 根据解析的参数，做本地业务
        boolean result = login(name, pwd);

        // 3. 根据业务方法执行的结果填写方法的响应值（这里做测试就直接填写）
        UserServiceProto.Response.Builder response_builder = UserServiceProto.Response.newBuilder();
        response_builder.setErrno(0);
        response_builder.setErrinfo("");
        response_builder.setResult(result);

        // 4. 把Response对象发送到nprpc框架，由框架负责处理发送rpc调用响应值
        done.run(response_builder.build());
    }

    @Override
    public void reg(RpcController controller, UserServiceProto.RegRequest request,
                    RpcCallback<UserServiceProto.Response> done) {
        // 1. 获取参数
        String name = request.getName();
        String pwd = request.getPwd();
        int age = request.getAge();
        UserServiceProto.RegRequest.SEX sex = request.getSex();
        String phone = request.getPhone();

        // 调用本地方法
        boolean result = reg(name, pwd, age, sex, phone);

        // 生成Response对象
        UserServiceProto.Response.Builder reg_response = UserServiceProto.Response.newBuilder();
        reg_response.setErrno(0);   //假设本地的注册方法调用成功
        reg_response.setErrinfo("");
        reg_response.setResult(result);

        // 将Response对象发送给rpc框架，由框架负责处理发送rpc调用响应
        done.run(reg_response.build());
    }
}
