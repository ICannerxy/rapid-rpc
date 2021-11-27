package com.kele.rpc.client;

/**
 * @author icanner
 * @date 2021/11/26:10:53 下午
 */
public class RpcClient {

    private String serverAddress;

    private long timeout;

    public RpcClient(String serverAddress, long timeout) {
        this.serverAddress = serverAddress;
        this.timeout = timeout;
        connect();
    }

    private void connect() {
        RpcConnectManager.getInstance().connect(serverAddress);
    }

    public void close() {
        RpcConnectManager.getInstance().stop();
    }
}
