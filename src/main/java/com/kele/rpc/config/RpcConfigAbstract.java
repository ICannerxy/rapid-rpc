package com.kele.rpc.config;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author icanner
 * @date 2021/11/28:7:52 下午
 */
public abstract class RpcConfigAbstract {

    private AtomicInteger generator = new AtomicInteger(0);

    protected  String id;

    protected  String interfaceClass = null;

    // 服务的调用方特有的属性
    protected Class<?> proxyClass = null;

    public String getId() {
        if (StringUtils.isEmpty(id)) {
            id = "rapid-cfg-gen-" + generator.getAndIncrement();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInterface(String interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public String getInterface() {
        return interfaceClass;
    }

}
