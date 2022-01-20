package com.kele.rpc.client;

import com.kele.rpc.codec.RpcRequest;
import com.kele.rpc.codec.RpcResponse;
import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author icanner
 * @date 2021/11/28:10:11 下午
 */
@Slf4j
public class RpcFuture implements Future<Object> {

    private RpcRequest request;

    private RpcResponse response;

    private long startTime;

    private Sync sync;

    private static final long TIME_THRESHOLD = 5000;

    private List<RpcCallback> pendingCallbacks = new ArrayList<>();

    private ReentrantLock lock = new ReentrantLock();

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));

    public RpcFuture(RpcRequest request) {
        this.request = request;
        this.startTime = System.currentTimeMillis();
        sync = new Sync();
    }

    /**
     * 实际的回调处理
     *
     * @param response
     */
    public void done(RpcResponse response) {
        this.response = response;
        boolean success = sync.release(1);
        if (success) {
            invokeCallbacks();
        }
        long costTime = System.currentTimeMillis() - startTime;
        if (costTime > TIME_THRESHOLD) {
            log.warn("the rpc response time is too slow, requestId: " + response.getRequestId() + " cost time: " + costTime);
        }
    }

    /**
     * 执行回调
     */
    private void invokeCallbacks() {
        lock.lock();
        try {
            for (final RpcCallback pendingCallback : pendingCallbacks) {
                runCallback(pendingCallback);
            }
        } finally {
            lock.unlock();
        }

    }

    private void runCallback(RpcCallback callback) {
        final RpcResponse response = this.response;
        threadPoolExecutor.submit(() -> {
            if (response.getThrowable() == null) {
                callback.success(response.getResult());
            } else {
                callback.failure(response.getThrowable());
            }
        });
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.tryAcquire(-1);
        if (this.response != null) {
            return this.response.getResult();
        } else {
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, timeout);
        if (success) {
            if (this.response != null) {
                return this.response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("timeout exception requestId: "
                    + this.request.getRequestId() + ",methodName: "
                    + this.request.getMethodName() + " ,className: "
                    + request.getClassName());
        }
    }

    public RpcFuture addCallback(RpcCallback callback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    class Sync extends AbstractQueuedSynchronizer {

        private final int done = 1;

        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int acquire) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int release) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isDone() {
            return getState() == done;
        }
    }


}
