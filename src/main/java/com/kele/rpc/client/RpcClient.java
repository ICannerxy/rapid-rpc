package com.kele.rpc.client;

import com.kele.rpc.client.proxy.ProxyImpl;
import com.kele.rpc.client.proxy.RpcAsyncProxy;
import com.kele.rpc.domain.HelloService;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author icanner
 * @date 2021/11/26:10:53 下午
 */
public class RpcClient {

    private String serverAddress;

    private long timeout;

    private ConcurrentHashMap<Class<?>, Object> proxySyncInstanceMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Class<?>, Object> proxyASyncInstanceMap = new ConcurrentHashMap<>();


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

    public <T> T invokeSync(Class<T> interfaceClass) {
        if (proxySyncInstanceMap.containsKey(interfaceClass)) {
            return (T) proxySyncInstanceMap.get(interfaceClass);
        } else {
            Object proxy = Proxy.newProxyInstance(
                    interfaceClass.getClassLoader(),
                    new Class<?>[]{interfaceClass},
                    new ProxyImpl<>( interfaceClass, timeout));
            proxySyncInstanceMap.put(interfaceClass, proxy);
            return (T) proxy;
        }

    }

    public static void main(String[] args) {
        RpcClient client = new RpcClient("xx", 33);
        HelloService helloService = client.invokeSync(HelloService.class);
        System.out.println(helloService);
    }

    public <T> RpcAsyncProxy invokeAsync(Class<T> interfaceClass) {
        if (proxyASyncInstanceMap.containsKey(interfaceClass)) {
            return (RpcAsyncProxy) proxyASyncInstanceMap.get(interfaceClass);
        }
        ProxyImpl proxy = new ProxyImpl(interfaceClass, timeout);
        proxyASyncInstanceMap.put(interfaceClass, proxy);
        return proxy;

    }
}
