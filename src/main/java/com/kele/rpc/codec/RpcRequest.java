package com.kele.rpc.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * @author icanner
 * @date 2021/11/26:11:12 下午
 */
@Data
public class RpcRequest implements Serializable {

    private String requestId;

    private String className;

    private String method;

    private Class<?>[] parameterTypes;

    private Object[] parameters;
}
