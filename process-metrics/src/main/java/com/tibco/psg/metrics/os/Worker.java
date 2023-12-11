package com.tibco.psg.metrics.os;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final Map<String, AtomicLong> metrics = new HashMap<String, AtomicLong>();

    private Pattern commandLinePattern;

    public Worker(String cmdline_filter) {
        this.commandLinePattern = Pattern.compile(cmdline_filter);

        /**
         * Set up the global (composite) Metric registry. Provide the global config generically.
         */
        Metrics.globalRegistry.config();
    }

    private AtomicLong trackMetric(String category, String metric, List<Tag> tags, String... ids) {
        String internal_metric_id = category + "/" + metric + "/" + String.join("/", ids);

        AtomicLong m = metrics.get(internal_metric_id);
        if (m == null) {
            m = Metrics.gauge(
                    String.format("os.%s.%s", category, metric),
                    tags,
                    new AtomicLong(-1)
            );
            metrics.put(internal_metric_id, m);
        }

        return m;
    }

    @Override
    public void run() {
        try {
            ProcessHandle.allProcesses().forEach(p -> trackProcess(p));
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Something went wrong during the worker-run!", t);
        }
    }

    private void trackProcess(ProcessHandle ph) {
        if (ph.info().command().isPresent()) {
            Matcher m = this.commandLinePattern.matcher(ph.info().command().get());
            if (m.matches()) {
                Instant start = ph.info().startInstant().orElse(Instant.EPOCH);
                String user = ph.info().user().orElse("unknown");
                String command = ph.info().command().orElse("unknown");
                String arguments = ph.info().arguments().isPresent() ? String.join(" ", ph.info().arguments().get()) : "unknown";

                List<Tag> tags = Arrays.asList(
                        Tag.of("start", start.toString()),
                        Tag.of("user", user),
                        Tag.of("command", command),
                        Tag.of("arguments", arguments)
                );

                Duration totalCpuDuration = ph.info().totalCpuDuration().orElse(Duration.ZERO);

                trackMetric(
                        "process",
                        "total_cpu_duration",
                        tags,
                        start.toString(),user,command,arguments
                ).set(totalCpuDuration.toMillis());
            }
        }
    }
}