package util;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 和zookeeper通信用的工具类
 */
public class ZKClinetUtils {
    private static String rootPath = "/nprpc";

    private ZkClient zkClient;

    private Map<String, String> ephemeralMap = new HashMap<>();

    /**
     * 通过zk server 字符串信息 连接zk server
     * @param serverList
     */
    public ZKClinetUtils(String serverList) {
        this.zkClient = new ZkClient(serverList, 3000);

        // 如果root节点不存在，则创建
        if (!this.zkClient.exists(rootPath)) {
            this.zkClient.createPersistent(rootPath, null);
        }
    }

    /**
     * 关闭和zk server的连接
     */
    public void close() {
        this.zkClient.close();
    }

    /**
     * zk上创建临时性节点, zk客户端断开后，节点被关闭
     * @param path
     * @param data  方法对应的 ip:port
     */
    public void createEphemeral(String path, String data) {
        path = rootPath + path;
        ephemeralMap.put(path, data);
        // znode节点不存在才创建
        if (!this.zkClient.exists(path)) {
            this.zkClient.createEphemeral(path, data);
        }
    }

    /**
     * 创建永久性节点
     * @param path
     * @param data
     */
    public void createPersistent(String path, String data) {
        path = rootPath + path;
        if (!this.zkClient.exists(path)) {
            this.zkClient.createPersistent(path, data);
        }
    }

    /**
     * 读取znode 节点的值
     * @param path
     * @return
     */
    public String readData(String path) {
        if (!this.zkClient.exists(rootPath + path)) {
            return null;
        }

        return this.zkClient.readData(rootPath + path, null);
    }

    /**
     * 在zk上指定的znode节点添加watcher监听
     * @param path
     */
    public void addWatcher(String path) {
        this.zkClient.subscribeDataChanges(rootPath + path, new IZkDataListener(){

            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {

            }

            /**
             * 设置节点znode监听，
             * 如果zkClint断掉，由于zkServer无法及时得知zkClient的关闭状态，zkServer会等待timeout时间后，
             * 会把zkClint创建的临时节点删除掉。刚断掉zkClient还在timeout时间内，又启动了zkClint，由于之前的
             * 节点还未删除，不会创建.
             * @param dataPath
             * @throws Exception
             */
            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                System.out.println("watcher - handleDataDeleted: " + dataPath);
                // 将删除的临时znode节点重新创建
                String data = ephemeralMap.get(dataPath);
                if (data != null) {
                    // 这里需要调用原始方法，上面重载的方法会修改路径
                    zkClient.createEphemeral(dataPath, data);
                }
            }
        });
    }

    public static String getRootPath() {
        return rootPath;
    }

    public static void setRootPath(String rootPath) {
        ZKClinetUtils.rootPath = rootPath;
    }

    /**
     * ZKClientUtils类测试
     * @param args
     */
//    public static void main(String[] args) {
//        ZKClinetUtils zk = new ZKClinetUtils("127.0.0.1:2181");
//        zk.createPersistent("/ProductService", "123456");
//
//        System.out.println(zk.readData("/ProductService"));
//        zk.close();
//    }
}
