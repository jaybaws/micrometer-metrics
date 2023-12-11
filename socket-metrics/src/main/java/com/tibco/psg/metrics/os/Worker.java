package com.tibco.psg.metrics.os;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final Map<Integer, AtomicLong> metrics = new HashMap<Integer, AtomicLong>();

    public Worker(List<Integer> ports) {
        /**
         * Set up the global (composite) Metric registry. Provide the global config generically.
         */
        Metrics.globalRegistry.config();

        /**
         * Initialize the map of unique metrics. This is deterministic, since it only depends
         * on the list of provided ports
         */
        for (int port : ports) {
            metrics.put(
                    port,
                    Metrics.gauge(
                            String.format("os.socket.is_open"),
                            Arrays.asList(
                                    Tag.of("port", String.valueOf(port))
                            ),
                            new AtomicLong(-1)
                    )
            );
        }
    }

    @Override
    public void run() {
        try {
            for (int port : this.metrics.keySet()) {
                AtomicLong metric = metrics.get(port);
                // metric.set(123)
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Something went wrong during the worker-run!", t);
        }
    }

}