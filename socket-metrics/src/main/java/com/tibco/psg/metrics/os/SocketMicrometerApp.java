package com.tibco.psg.metrics.os;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SocketMicrometerApp {

    private static final int c_executorService_corePoolSize = 10;

    private static final String c_jvm_arg_prefix = SocketMicrometerApp.class.getPackage().getName();

    private static final String c_jvm_arg_ports = c_jvm_arg_prefix + ".ports";

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(c_executorService_corePoolSize);

    public static void main(String[] args) {
        LOGGER.info("Starting OS Sockets Micrometer metrics application...");

        String ports = System.getProperty(c_jvm_arg_ports, "");
        List<Integer> port_ints = Arrays.stream(ports.split("\\s"))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        if (port_ints.size() > 0) {
            Worker worker = new Worker(port_ints);

            executorService.scheduleWithFixedDelay(
                    worker,
                    0,
                    60,
                    TimeUnit.SECONDS
            );

            LOGGER.info("Worker scheduled!");
        } else {
            LOGGER.warning("No port numbers were provided. Nothing to do!");
        }
    }
}