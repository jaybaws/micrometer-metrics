package org.jaybaws.metrics.os;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final Map<Integer, Long> metrics = new HashMap<Integer, Long>();

    public Worker(List<Integer> ports) {
        OpenTelemetry sdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
        Meter meter = sdk.getMeter("sockets");

        /*
         * Initialize the map of unique metrics. This is deterministic, since it only depends
         * on the list of provided ports
         */
        for (int port : ports) {
            this.metrics.put(port, (long) -1);
            meter
                    .gaugeBuilder("os.socket.is_open")
                    .ofLongs()
                    .buildWithCallback(
                            result -> result.record(
                                    this.metrics.get(port),
                                    Attributes.builder()
                                            .put("port", String.valueOf(port))
                                            .build()
                            )
                    );
        }
    }

    @Override
    public void run() {
        try {
            for (int port : this.metrics.keySet()) {
                boolean open = !available(port);
                long value = (open) ? 1 : 0;
                metrics.replace(port, value);

            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Something went wrong during the worker-run!", t);
        }
    }

    public static boolean available(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            /* yeah whatever */
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

}