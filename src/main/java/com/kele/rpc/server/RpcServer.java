package com.kele.rpc.server;

import com.kele.rpc.codec.RpcDecoder;
import com.kele.rpc.codec.RpcEncoder;
import com.kele.rpc.codec.RpcRequest;
import com.kele.rpc.codec.RpcResponse;
import com.kele.rpc.config.provider.ProviderConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author icanner
 * @date 2021/11/28:2:06 下午
 */
@Slf4j
public class RpcServer {

    private String serverAddress;

    private EventLoopGroup boosGroup = new NioEventLoopGroup();
    private EventLoopGroup workGroup = new NioEventLoopGroup();

    /**
     * key: 接口 value：接口实现类
     */
    private volatile Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serverAddress) throws InterruptedException {
        this.serverAddress = serverAddress;
        start();
    }

    private void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boosGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
                        ch.pipeline().addLast(new RpcDecoder(RpcRequest.class))
                                .addLast(new RpcEncoder(RpcResponse.class))
                                .addLast(new RpcServerHandler(handlerMap));
                    }
                });
        String[] array = serverAddress.split(":");
        String host = array[0];
        int port = Integer.parseInt(array[1]);
        ChannelFuture channelFuture = bootstrap.bind(host, port).sync();
        channelFuture.addListener((ChannelFutureListener) future -> {
           if (future.isSuccess()) {
                log.info("server success binding to " + serverAddress);
           } else {
               log.info("server fail binding to " + serverAddress);
               throw new Exception("server start fail, cause: " + future.cause());
           }
        });

        // 同步等待结果
//        channelFuture.awaitUninterruptibly(5000, TimeUnit.MICROSECONDS);
//        if (channelFuture.isSuccess()) {
//            log.info("server success binding to " + serverAddress);
//        }

    }


    /**
     * 程序注册器
     */
    public void registerProcessor(ProviderConfig config) {
        // key： userService接口全限定名
        // value：providerConfig.ref() userService接口下具体实现类
        handlerMap.put(config.getInterface(), config.getRef());
    }

    public void close() {
        boosGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
    }

}
