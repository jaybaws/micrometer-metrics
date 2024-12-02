package org.jaybaws.metrics.os;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ProcessMonitorApp {

    private static final int c_executorService_corePoolSize = 5;

    private static final String c_jvm_arg_prefix = ProcessMonitorApp.class.getPackage().getName();

    private static final String c_jvm_arg_os_cmdline_filter = c_jvm_arg_prefix + ".cmdline_filter";

    private static final Logger LOGGER = Logger.getLogger(ProcessMonitorApp.class.getName());

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(c_executorService_corePoolSize);

    public static void main(String[] args) {
        LOGGER.info("Starting ProcessMonitorApp application...");

        String binary_filter = System.getProperty(c_jvm_arg_os_cmdline_filter, ".*");

        Worker worker = new Worker(binary_filter);

        executorService.scheduleWithFixedDelay(
                worker,
                0,
                60,
                TimeUnit.SECONDS
        );

        LOGGER.info("Worker scheduled!");
    }
}