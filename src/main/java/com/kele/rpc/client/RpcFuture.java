package com.kele.rpc.client;

import com.kele.rpc.codec.RpcRequest;
import com.kele.rpc.codec.RpcResponse;
import com.sun.corba.se.impl.orbutil.concurrent.Sync;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author icanner
 * @date 2021/11/28:10:11 下午
 */
public class RpcFuture implements Future<Object> {

    private RpcRequest request;

    private RpcResponse response;

    private long startTime;

    private Sync sync;

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
    }

    /**
     * 执行回调
     */
    private void invokeCallbacks() {
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

    class Sync extends AbstractQueuedSynchronizer {

        private final int done = 1;

        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int acquire) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int release) {
            if (release == pending) {
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
