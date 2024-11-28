package org.jaybaws.metrics.bw.metrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import org.jaybaws.metrics.bw.BW5MicrometerAgent;
import java.util.logging.Logger;

public class JVM {

    private static final Logger LOGGER = Logger.getLogger(BW5MicrometerAgent.class.getName());

    public static void instrument(OpenTelemetry sdk) {
        io.opentelemetry.api.metrics.Meter jvmMeter = sdk.getMeter("com.tibco.bw.jvm");
        jvmMeter
                .gaugeBuilder("jvm.memory.total")
                .setDescription("Reports JVM memory usage.")
                .setUnit("byte")
                .buildWithCallback(result -> result.record(Runtime.getRuntime().totalMemory(), Attributes.empty()));

        LOGGER.info("Created JVM metrics!");
    }

}