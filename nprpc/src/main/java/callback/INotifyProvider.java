package callback;

public interface INotifyProvider {

    /**
     * 回调操作，RpcServer给RpcProvider上报接收到的rpc服务调用相关参数信息
     * @param serviceName
     * @param methodName
     * @param args
     * @return 把rpc调用完成后的响应返回
     */
    byte[] notify(String serviceName, String methodName, byte[] args);
}
