package com.kele.rpc.domain;

public interface HelloService {

    String hello(String name);

    String hello(User user);

}
