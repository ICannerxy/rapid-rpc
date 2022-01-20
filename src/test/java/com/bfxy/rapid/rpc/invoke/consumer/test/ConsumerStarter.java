package com.bfxy.rapid.rpc.invoke.consumer.test;

import com.kele.rpc.client.RpcClient;
import com.kele.rpc.client.RpcFuture;
import com.kele.rpc.client.proxy.RpcAsyncProxy;
import com.kele.rpc.domain.HelloService;
import com.kele.rpc.domain.User;

import java.util.concurrent.ExecutionException;


public class ConsumerStarter {
	
	public static void sync() {
		//	rpcClient
		RpcClient rpcClient = new RpcClient("127.0.0.1:8765", 3000);
		HelloService helloService = rpcClient.invokeSync(HelloService.class);
		String result = helloService.hello("zhang3");
		System.err.println(result);		
	}
	
	public static void async() throws InterruptedException, ExecutionException {
		RpcClient rpcClient = new RpcClient("127.0.0.1:8765", 3000);
		RpcAsyncProxy proxy = rpcClient.invokeAsync(HelloService.class);
		RpcFuture future = proxy.call("hello", "li4");
		RpcFuture future2 = proxy.call("hello", new User("001", "wang5"));

		Object result = future.get();
		Object result2 = future2.get();
		System.err.println("result: " + result);
		System.err.println("result2: " + result2);

	}
	
	public static void main(String[] args) throws Exception {
		sync();
	}
}
