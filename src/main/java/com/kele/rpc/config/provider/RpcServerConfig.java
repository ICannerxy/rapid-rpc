package com.kele.rpc.config.provider;

import com.kele.rpc.server.RpcServer;

import java.util.List;

/**
 * @author icanner
 * @date 2021/11/29:10:05 下午
 */
public class RpcServerConfig {

    private String host = "127.0.0.1";

    protected int port;

    private List<ProviderConfig> providerConfigs;

    private RpcServer rpcServer;

    public RpcServerConfig(List<ProviderConfig> providerConfigs) {
        this.providerConfigs = providerConfigs;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void exporter() {
        if (rpcServer == null) {
            try {
                rpcServer = new RpcServer(host + ":" + port);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (ProviderConfig providerConfig : providerConfigs) {
                rpcServer.registerProcessor(providerConfig);
            }

        }
    }
}
