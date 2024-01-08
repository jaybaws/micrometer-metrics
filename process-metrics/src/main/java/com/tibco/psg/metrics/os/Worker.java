package com.tibco.psg.metrics.os;
import org.json.JSONObject;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private Pattern commandLinePattern;

    public Worker(String cmdline_filter) {
        this.commandLinePattern = Pattern.compile(cmdline_filter);
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
                String user = ph.info().user().orElse("");
                String cmd = ph.info().command().orElse("");
                String cmdline = ph.info().commandLine().orElse("");
                String arguments = ph.info().arguments().isPresent() ? String.join(" ", ph.info().arguments().get()) : "";

                Duration totalCpuDuration = ph.info().totalCpuDuration().orElse(Duration.ZERO);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("pid", ph.pid());
                jsonObject.put("start", start.getEpochSecond());
                jsonObject.put("user", user);
                jsonObject.put("cmd", cmd);
                jsonObject.put("cmdline", cmdline);
                jsonObject.put("arguments", arguments);
                jsonObject.put("duration", totalCpuDuration.toMillis());

                String logMsg = jsonObject.toString();

                LOGGER.info(logMsg);
            }
        }
    }
}