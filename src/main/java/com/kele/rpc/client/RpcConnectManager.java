package com.kele.rpc.client;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author icanner
 * @date 2021/9/127:27 下午
 */
@Slf4j
public class RpcConnectManager {

    private static volatile RpcConnectManager RPC_CONNECT_MANAGER = new RpcConnectManager();

    /* 用于异步提交创建连接任务*/
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));


    /** 一个连接的地址，对应一个业务处理器*/
    private ConcurrentHashMap<InetSocketAddress, RpcClientHandler> connectHandleMap = new ConcurrentHashMap<>();

    /** 所有连接成功的地址所对应的任务执行器列表*/
    private CopyOnWriteArrayList<RpcClientHandler> connectHandleList = new CopyOnWriteArrayList<>();

    private ReentrantLock connectLock = new ReentrantLock();

    private Condition connectCondition = connectLock.newCondition();

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private RpcConnectManager() {

    }

    public static RpcConnectManager getInstance() {
        return RPC_CONNECT_MANAGER;
    }

    // 1. 异步创建连接 线程池 真正发起连接处理连接失败和成功事件
    // 2. 对于连接进来的资源（做一个管理）updateConnectedServer
    public void connect(final String serverAddress) {
        List<String> allServerAddress = Arrays.asList(serverAddress.split(","));
        updateConnectedServer(allServerAddress);
    }

    /**
     *
     * @param allServerAddress
     */
    private void updateConnectedServer(List<String> allServerAddress) {
        Set<InetSocketAddress> newAllServerNodeSet = new HashSet<>();
        if (CollectionUtils.isEmpty(allServerAddress)) {
            log.error("no available server address! ");
            // 清除所有的连接信息
            clearConnected();
        }
        for (String serverAddress : allServerAddress) {
            String[] addresses = serverAddress.split(":");
            if (addresses.length == 2) {
                String host = addresses[0];
                int port = Integer.parseInt(addresses[1]);
                final InetSocketAddress address = new InetSocketAddress(host, port);
                newAllServerNodeSet.add(address);
            }

        }
        // 异步建立连接
        for (InetSocketAddress remotePeer : newAllServerNodeSet) {
            if (!connectHandleMap.containsKey(remotePeer)) {
                connectAsync(remotePeer);
            }

        }

        // 如果AllServerAddress里不存在的地址，需要进行删除
        for (int i = connectHandleList.size() - 1; i >= 0; i--) {
            RpcClientHandler rpcClientHandler = connectHandleList.get(i);
            InetSocketAddress remotePeer = rpcClientHandler.getRemotePeer();
            if (!newAllServerNodeSet.contains(remotePeer)) {
                log.info("remove invalid server node :{}", remotePeer);
                RpcClientHandler clientHandler = connectHandleMap.get(remotePeer);
                if (clientHandler != null) {
                    clientHandler.close();
                    connectHandleMap.remove(remotePeer);
                }
                connectHandleList.remove(i);
            }
        }


    }

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        for (int i = 0; i < list.size(); i++) {
            Integer integer = list.get(i);
            if (integer.equals(2) || integer.equals(3)) {
                list.remove(integer);
            }
        }
    }

    /**
     * 异步发起连接
     *
     * @param remotePeer
     */
    private void connectAsync(InetSocketAddress remotePeer) {
        threadPoolExecutor.submit(() -> {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new RpcClientInitializer());

            connect(bootstrap, remotePeer);
        });
    }

    private void connect(final Bootstrap bootstrap, final InetSocketAddress remotePeer) {
        // 真正的建立连接
        ChannelFuture channelFuture = bootstrap.connect(remotePeer);
        channelFuture.channel().closeFuture().addListener(future -> {
            log.warn("channelFuture.channel connect error, remotePeer: {}", remotePeer);
            channelFuture.channel().eventLoop().schedule(() -> {
                log.warn("connect filed, to reconnect!");
                clearConnected();
                connect(bootstrap, remotePeer);
            }, 3, TimeUnit.SECONDS);
        });
        // 处理成功连接情况
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                log.info("client channel connect {} success.", remotePeer);
            }
            addChannel(channelFuture.channel().pipeline().get(RpcClientHandler.class));
        });
    }

    /**
     * 添加rpcClientHandler到指定缓存中
     *
     * @param rpcClientHandler
     */
    private void addChannel(RpcClientHandler rpcClientHandler) {
        connectHandleMap.put(rpcClientHandler.getRemotePeer(), rpcClientHandler);
        connectHandleList.add(rpcClientHandler);
        // 唤醒可用的业务处理器
        signalAvailableHandle();
    }

    /**
     * 唤醒另一端阻塞的线程，有新的连接可用
     */
    private void signalAvailableHandle() {
        connectLock.lock();
        try {
            connectCondition.signalAll();
        } finally {
            connectLock.unlock();
        }
    }

    /**
     * 清除所有连接
     */
    private void clearConnected() {
        for (RpcClientHandler rpcClientHandler : connectHandleList) {
            InetSocketAddress remotePeer = rpcClientHandler.getRemotePeer();
            RpcClientHandler handler = connectHandleMap.get(remotePeer);
            if (null != handler) {
                handler.close();
                connectHandleMap.remove(remotePeer);
            }
        }
        connectHandleList.clear();

    }


}
