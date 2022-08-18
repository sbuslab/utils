package com.sbuslab.utils.config.memcached;

import net.spy.memcached.MemcachedNode;
import net.spy.memcached.NodeLocator;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class NoopNodeLocator implements NodeLocator {

    @Override
    public MemcachedNode getPrimary(String k) {
        return null;
    }

    @Override
    public Iterator<MemcachedNode> getSequence(String k) {
        return null;
    }

    @Override
    public Collection<MemcachedNode> getAll() {
        return Collections.emptySet();
    }

    @Override
    public NodeLocator getReadonlyCopy() {
        return null;
    }

    @Override
    public void updateLocator(List<MemcachedNode> nodes) {

    }
}
