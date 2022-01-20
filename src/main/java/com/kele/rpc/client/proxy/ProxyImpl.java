package com.kele.rpc.client.proxy;

import com.kele.rpc.client.RpcClientHandler;
import com.kele.rpc.client.RpcConnectManager;
import com.kele.rpc.client.RpcFuture;
import com.kele.rpc.codec.RpcRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author icanner
 * @date 2021/11/29:9:21 下午
 */
public class ProxyImpl<T> implements InvocationHandler, RpcAsyncProxy {

    private Class<T> interfaceClass;

    private long timeout;

    public ProxyImpl(Class<T> interfaceClass, long timeout) {
        this.interfaceClass = interfaceClass;
        this.timeout = timeout;
    }

    /**
     * 代理接口调用
     *
     * @param o
     * @param method
     * @param objects
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        // 设置请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(objects);
        request.setMethodName(method.getName());

        RpcClientHandler rpcClientHandler = RpcConnectManager.getInstance().chooseHandler();
        RpcFuture rpcFuture = rpcClientHandler.sendRequest(request);
        return rpcFuture.get(timeout, TimeUnit.SECONDS);
    }

    @Override
    public RpcFuture call(String funcName, Object... args) {
        // 设置请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(this.interfaceClass.getName());
        request.setParameters(args);
        request.setMethodName(funcName);
        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            Class<?> parameterType = getClassType(args[i]);
            parameterTypes[0] = parameterType;
        }
        request.setParameterTypes(parameterTypes);

        RpcClientHandler rpcClientHandler = RpcConnectManager.getInstance().chooseHandler();
        RpcFuture rpcFuture = rpcClientHandler.sendRequest(request);
        return rpcFuture;
    }

    private Class<?> getClassType(Object obj) {
        Class<?> classType = obj.getClass();
        String typeName = classType.getName();
        if (typeName.equals("java.lang.Integer")) {
            return Integer.TYPE;
        } else if (typeName.equals("java.lang.Long")) {
            return Long.TYPE;
        } else if (typeName.equals("java.lang.Float")) {
            return Float.TYPE;
        } else if (typeName.equals("java.lang.Double")) {
            return Double.TYPE;
        } else if (typeName.equals("java.lang.Character")) {
            return Character.TYPE;
        } else if (typeName.equals("java.lang.Boolean")) {
            return Boolean.TYPE;
        } else if (typeName.equals("java.lang.Short")) {
            return Short.TYPE;
        } else if (typeName.equals("java.lang.Byte")) {
            return Byte.TYPE;
        }
        return classType;
    }
}
