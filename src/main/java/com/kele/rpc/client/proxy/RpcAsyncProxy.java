package com.kele.rpc.client.proxy;

import com.kele.rpc.client.RpcFuture;

/**
 * @author icanner
 * @date 2021/11/29:9:47 下午
 */
public interface RpcAsyncProxy {

    RpcFuture call(String funcName, Object... args);
}
