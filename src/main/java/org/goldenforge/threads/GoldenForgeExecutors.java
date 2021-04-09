package org.goldenforge.threads;

public class GoldenForgeExecutors {

    public static final java.util.concurrent.ExecutorService CHAT = java.util.concurrent.Executors.newCachedThreadPool(
            new com.google.common.util.concurrent.ThreadFactoryBuilder().setDaemon( true ).setNameFormat( "Async Chat Thread - #%d" ).build() );
}
