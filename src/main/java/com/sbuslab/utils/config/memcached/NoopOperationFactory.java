package com.sbuslab.utils.config.memcached;

import net.spy.memcached.OperationFactory;
import net.spy.memcached.ops.*;
import net.spy.memcached.tapmessage.RequestMessage;
import net.spy.memcached.tapmessage.TapOpcode;

import javax.security.auth.callback.CallbackHandler;
import java.util.Collection;
import java.util.Map;

public class NoopOperationFactory implements OperationFactory {
    @Override
    public NoopOperation noop(OperationCallback cb) {
        return null;
    }

    @Override
    public DeleteOperation delete(String key, DeleteOperation.Callback callback) {
        return null;
    }

    @Override
    public DeleteOperation delete(String key, long cas, DeleteOperation.Callback callback) {
        return null;
    }

    @Override
    public UnlockOperation unlock(String key, long casId, OperationCallback operationCallback) {
        return null;
    }

    @Override
    public ObserveOperation observe(String key, long casId, int index, ObserveOperation.Callback operationCallback) {
        return null;
    }

    @Override
    public FlushOperation flush(int delay, OperationCallback operationCallback) {
        return null;
    }

    @Override
    public GetAndTouchOperation getAndTouch(String key, int expiration, GetAndTouchOperation.Callback cb) {
        return null;
    }

    @Override
    public GetOperation get(String key, GetOperation.Callback callback) {
        return null;
    }

    @Override
    public ReplicaGetOperation replicaGet(String key, int index, ReplicaGetOperation.Callback callback) {
        return null;
    }

    @Override
    public ReplicaGetsOperation replicaGets(String key, int index, ReplicaGetsOperation.Callback callback) {
        return null;
    }

    @Override
    public GetlOperation getl(String key, int exp, GetlOperation.Callback callback) {
        return null;
    }

    @Override
    public GetsOperation gets(String key, GetsOperation.Callback callback) {
        return null;
    }

    @Override
    public GetOperation get(Collection<String> keys, GetOperation.Callback cb) {
        return null;
    }

    @Override
    public StatsOperation keyStats(String key, StatsOperation.Callback cb) {
        return null;
    }

    @Override
    public MutatorOperation mutate(Mutator m, String key, long by, long def, int exp, OperationCallback cb) {
        return null;
    }

    @Override
    public StatsOperation stats(String arg, StatsOperation.Callback cb) {
        return null;
    }

    @Override
    public StoreOperation store(StoreType storeType, String key, int flags, int exp, byte[] data, StoreOperation.Callback cb) {
        return null;
    }

    @Override
    public TouchOperation touch(String key, int expiration, OperationCallback cb) {
        return null;
    }

    @Override
    public ConcatenationOperation cat(ConcatenationType catType, long casId, String key, byte[] data, OperationCallback cb) {
        return null;
    }

    @Override
    public CASOperation cas(StoreType t, String key, long casId, int flags, int exp, byte[] data, StoreOperation.Callback cb) {
        return null;
    }

    @Override
    public VersionOperation version(OperationCallback cb) {
        return null;
    }

    @Override
    public SASLMechsOperation saslMechs(OperationCallback cb) {
        return null;
    }

    @Override
    public SASLAuthOperation saslAuth(String[] mech, String serverName, Map<String, ?> props, CallbackHandler cbh, OperationCallback cb) {
        return null;
    }

    @Override
    public SASLStepOperation saslStep(String[] mech, byte[] challenge, String serverName, Map<String, ?> props, CallbackHandler cbh, OperationCallback cb) {
        return null;
    }

    @Override
    public Collection<Operation> clone(KeyedOperation op) {
        return null;
    }

    @Override
    public TapOperation tapBackfill(String id, long date, OperationCallback cb) {
        return null;
    }

    @Override
    public TapOperation tapCustom(String id, RequestMessage message, OperationCallback cb) {
        return null;
    }

    @Override
    public TapOperation tapAck(TapOpcode opcode, int opaque, OperationCallback cb) {
        return null;
    }

    @Override
    public TapOperation tapDump(String id, OperationCallback cb) {
        return null;
    }
}
