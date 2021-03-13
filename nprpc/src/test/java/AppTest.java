import static org.junit.Assert.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Test;
import org.omg.CORBA.PUBLIC_MEMBER;

import java.io.IOException;
import java.util.Properties;

/**
 * Unit test for simple App.
 */
public class AppTest 
{

    /**
     * 测试protobuf的序列化和反序列化
     */
    @Test
    public void test1() throws InvalidProtocolBufferException {
        TestProto.LoginRequest.Builder loginBuilder = TestProto.LoginRequest.newBuilder();
        loginBuilder.setName("zhang san");
        loginBuilder.setPwd("12345");

        TestProto.LoginRequest request = loginBuilder.build();
        System.out.println(request.getName());
        System.out.println(request.getPwd());

        /**
         * 把loginRequest对象序列化成字节流，然后就可以通过网络发送出去了
         */
        byte[] sendbuf = request.toByteArray();

        /**
         * 反序列化
         */
        TestProto.LoginRequest r = TestProto.LoginRequest.parseFrom(sendbuf);
        System.out.println(r.getName());
        System.out.println(r.getPwd());
    }

    @Test
    public void test2() {
        Properties pro = new Properties();
        try {
            pro.load(AppTest.class.getClassLoader().getResourceAsStream("config.properties"));
            System.out.println(pro.get("IP"));
            System.out.println(pro.get("PORT"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
