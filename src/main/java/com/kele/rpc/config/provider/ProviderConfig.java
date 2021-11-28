package com.kele.rpc.config.provider;

import com.kele.rpc.config.RpcConfigAbstract;

/**
 * @author icanner
 * @date 2021/11/28:7:47 下午
 */
public class ProviderConfig extends RpcConfigAbstract {


   protected Object ref;

    public Object getRef() {
        return ref;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }
}
