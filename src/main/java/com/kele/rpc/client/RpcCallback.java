package com.kele.rpc.client;

/**
 * @author icanner
 * @date 2021/11/28:10:17 下午
 */
public interface RpcCallback {

    void success(Object result);

    void failure(Throwable throwable);
}
