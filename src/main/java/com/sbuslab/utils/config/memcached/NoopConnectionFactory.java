package com.sbuslab.utils.config.memcached;

import net.spy.memcached.*;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.metrics.MetricCollector;
import net.spy.memcached.metrics.MetricType;
import net.spy.memcached.ops.*;
import net.spy.memcached.tapmessage.RequestMessage;
import net.spy.memcached.tapmessage.TapOpcode;
import net.spy.memcached.transcoders.Transcoder;

import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * A {@link ConnectionFactory} which allows not to connect to a memcached instance.
 * Useful in cases where caching is disabled.
 */
public class NoopConnectionFactory implements ConnectionFactory {

    @Override
    public MemcachedConnection createConnection(List<InetSocketAddress> addrs) throws IOException {
        return new MemcachedConnection(1, this,
                Collections.emptyList(), Collections.emptyList(),
                FailureMode.Cancel, getOperationFactory());
    }

    @Override
    public MemcachedNode createMemcachedNode(SocketAddress sa, SocketChannel c, int bufSize) {
        return null;
    }

    @Override
    public BlockingQueue<Operation> createOperationQueue() {
        return null;
    }

    @Override
    public BlockingQueue<Operation> createReadOperationQueue() {
        return null;
    }

    @Override
    public BlockingQueue<Operation> createWriteOperationQueue() {
        return null;
    }

    @Override
    public long getOpQueueMaxBlockTime() {
        return 0;
    }

    @Override
    public ExecutorService getListenerExecutorService() {
        return null;
    }

    @Override
    public boolean isDefaultExecutorService() {
        return false;
    }

    @Override
    public NodeLocator createLocator(List<MemcachedNode> nodes) {
        return new NoopNodeLocator();
    }

    @Override
    public OperationFactory getOperationFactory() {
        return new NoopOperationFactory();
    }

    @Override
    public long getOperationTimeout() {
        return 1L;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public boolean useNagleAlgorithm() {
        return false;
    }

    @Override
    public Collection<ConnectionObserver> getInitialObservers() {
        return null;
    }

    @Override
    public FailureMode getFailureMode() {
        return null;
    }

    @Override
    public Transcoder<Object> getDefaultTranscoder() {
        return null;
    }

    @Override
    public boolean shouldOptimize() {
        return false;
    }

    @Override
    public int getReadBufSize() {
        return 0;
    }

    @Override
    public HashAlgorithm getHashAlg() {
        return null;
    }

    @Override
    public long getMaxReconnectDelay() {
        return 0;
    }

    @Override
    public AuthDescriptor getAuthDescriptor() {
        return null;
    }

    @Override
    public int getTimeoutExceptionThreshold() {
        return 0;
    }

    @Override
    public MetricType enableMetrics() {
        return MetricType.OFF;
    }

    @Override
    public MetricCollector getMetricCollector() {
        return null;
    }

    @Override
    public long getAuthWaitTime() {
        return 0;
    }
}