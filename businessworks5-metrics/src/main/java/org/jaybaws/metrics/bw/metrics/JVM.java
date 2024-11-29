package org.jaybaws.metrics.bw.metrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import org.jaybaws.metrics.bw.util.Logger;

public class JVM {

    public static void instrument(OpenTelemetry sdk) {
        Meter jvmMeter = sdk.getMeter("com.tibco.bw.jvm");
        jvmMeter
                .upDownCounterBuilder("jvm.memory.total")
                .setDescription("Reports JVM memory usage.")
                .setUnit("byte")
                .buildWithCallback(result -> result.record(Runtime.getRuntime().totalMemory(), Attributes.empty()));

        jvmMeter
                .upDownCounterBuilder("jvm.cpu.count")
                .setDescription("Number of processors available to the Java virtual machine.")
                .buildWithCallback(result -> result.record(Runtime.getRuntime().availableProcessors(), Attributes.empty()));

        for (MemoryPoolMXBean mpmxb : ManagementFactory.getMemoryPoolMXBeans()) {
            String memoryPoolName = mpmxb.getName();
            String memoryPoolType = mpmxb.getType().name();

            jvmMeter
                    .upDownCounterBuilder("jvm.memory.used")
                    .setDescription("Measure of memory used.")
                    .setUnit("byte")
                    .buildWithCallback(
                            result -> result.record(
                                    mpmxb.getUsage().getUsed(),
                                    Attributes.builder()
                                            .put("jvm.memory.pool.name", memoryPoolName)
                                            .put("jvm.memory.type", memoryPoolType)
                                            .build())
                    );

            jvmMeter
                    .upDownCounterBuilder("jvm.memory.committed")
                    .setDescription("Measure of memory committed.")
                    .setUnit("byte")
                    .buildWithCallback(
                            result -> result.record(
                                    mpmxb.getUsage().getCommitted(),
                                    Attributes.builder()
                                            .put("jvm.memory.pool.name", memoryPoolName)
                                            .put("jvm.memory.type", memoryPoolType)
                                            .build())
                    );

            jvmMeter
                    .upDownCounterBuilder("jvm.memory.limit")
                    .setDescription("Measure of max obtainable memory.")
                    .setUnit("byte")
                    .buildWithCallback(
                            result -> result.record(
                                    mpmxb.getUsage().getMax(),
                                    Attributes.builder()
                                            .put("jvm.memory.pool.name", memoryPoolName)
                                            .put("jvm.memory.type", memoryPoolType)
                                            .build())
                    );

            jvmMeter
                    .upDownCounterBuilder("jvm.memory.init")
                    .setDescription("Measure of initial memory requested.")
                    .setUnit("byte")
                    .buildWithCallback(
                            result -> result.record(
                                    mpmxb.getUsage().getInit(),
                                    Attributes.builder()
                                            .put("jvm.memory.pool.name", memoryPoolName)
                                            .put("jvm.memory.type", memoryPoolType)
                                            .build())
                    );
        }


        jvmMeter
                .upDownCounterBuilder("jvm.memory.heap.used")
                .buildWithCallback(
                        result -> result.record(
                                ManagementFactory.getPlatformMXBean(java.lang.management.MemoryMXBean.class).getHeapMemoryUsage().getUsed(),
                                Attributes.empty()
                        )
                );

        jvmMeter
                .upDownCounterBuilder("jvm.memory.heap.max")
                .buildWithCallback(
                        result -> result.record(
                                ManagementFactory.getPlatformMXBean(java.lang.management.MemoryMXBean.class).getHeapMemoryUsage().getMax(),
                                Attributes.empty()
                        )
                );

        jvmMeter.upDownCounterBuilder("jvm.memory.non_heap.used")
                .buildWithCallback(
                        result -> result.record(
                                ManagementFactory.getPlatformMXBean(java.lang.management.MemoryMXBean.class).getNonHeapMemoryUsage().getMax(),
                                Attributes.empty()
                        )
                );

        jvmMeter.upDownCounterBuilder("jvm.thread.count")
                .buildWithCallback(
                        result -> result.record(ManagementFactory.getThreadMXBean().getThreadCount(),
                                Attributes.empty()
                        )
                );

        for (GarbageCollectorMXBean gcmxb : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = gcmxb.getName();
            jvmMeter.upDownCounterBuilder("jvm.gc.count")
                    .buildWithCallback(
                            result -> result.record(gcmxb.getCollectionCount(),
                                    Attributes.builder().put("name", name).build()
                            )
                    );
            jvmMeter.upDownCounterBuilder("jvm.gc.time")
                    .buildWithCallback(
                            result -> result.record(gcmxb.getCollectionTime(),
                                    Attributes.builder().put("name", name).build()
                            )
                    );
        }

        /*
         * @TODO! How to determin this on Java 11 ???
         */
        jvmMeter.upDownCounterBuilder("system.cpu.total.norm.pct").buildWithCallback(result -> result.record(0, Attributes.empty()));

        Logger.info("Registered (async) JVM metrics!");
    }
}