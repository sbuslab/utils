package com.sbuslab.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public class FutureUtils {

    public static CompletableFuture<Void> chain(List<CompletableFuture<?>> fs) {
        return CompletableFuture.allOf(fs.toArray(new CompletableFuture[fs.size()]));
    }
}
