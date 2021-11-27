package com.kele.rpc.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * @author icanner
 * @date 2021/11/23:10:09 下午
 */
@Data
public class RpcResponse implements Serializable {

    private String requestId;

    private Object result;

    private Throwable throwable;


}
