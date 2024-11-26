package org.jaybaws.metrics.os;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketMicrometerApp {

    private static final int c_executorService_corePoolSize = 10;

    private static final String c_jvm_arg_prefix = SocketMicrometerApp.class.getPackage().getName();

    private static final String c_jvm_arg_ports = c_jvm_arg_prefix + ".ports";

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(c_executorService_corePoolSize);

    public static void main(String[] args) {
        LOGGER.info("Starting OS Sockets Micrometer metrics application...");

        String ports = System.getProperty(c_jvm_arg_ports, "443,10400-10499");
        List<Integer> port_ints = new ArrayList<Integer>();

        try {
            for (String s : ports.split(",")) {
                if (s.contains("-")) {
                    String[] range = s.split("-");
                    int begin = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    for (int i = begin; i <= end; i++) {
                        port_ints.add(i);
                    }
                } else {
                    port_ints.add(Integer.parseInt(s));
                }
            }

            LOGGER.info(String.format("Parsed ports: %s.", port_ints.toString()));

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

        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE,String.format("Unable to parse the provided port number(s): '%s'. Exiting!", ports), e);
        }
    }
}