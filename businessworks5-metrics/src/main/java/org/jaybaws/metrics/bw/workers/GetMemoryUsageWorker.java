package org.jaybaws.metrics.bw.workers;
import org.jaybaws.metrics.bw.BW5MicrometerAgent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetMemoryUsageWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(BW5MicrometerAgent.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    private AtomicLong metricMemUsed = Metrics.gauge("bwengine.memory.used", Arrays.asList(Tag.of("method", "GetMemoryUsage")), new AtomicLong(-1));
    private AtomicLong metricMemUsedPct = Metrics.gauge("bwengine.memory.used.pct", Arrays.asList(Tag.of("method", "GetMemoryUsage")), new AtomicLong(-1));
    private AtomicLong metricMemFree = Metrics.gauge("bwengine.memory.free", Arrays.asList(Tag.of("method", "GetMemoryUsage")), new AtomicLong(-1));
    private AtomicLong metricMemTotal = Metrics.gauge("bwengine.memory.total", Arrays.asList(Tag.of("method", "GetMemoryUsage")), new AtomicLong(-1));

    public GetMemoryUsageWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            CompositeDataSupport result = (CompositeDataSupport) mbsc.invoke(objectName, "GetMemoryUsage", null, null);

            if (result != null) {
                long valUsedBytes = (Long) result.get("UsedBytes");
                long valPercentUsed = (Long) result.get("PercentUsed");
                long valFreeBytes = (Long) result.get("FreeBytes");
                long valTotalBytes = (Long) result.get("TotalBytes");

                metricMemUsed.set(valUsedBytes);
                metricMemUsedPct.set(valPercentUsed);
                metricMemFree.set(valFreeBytes);
                metricMemTotal.set(valTotalBytes);

                LOGGER.fine(
                        String.format(
                                "[GetMemoryUsage] used=%d, free=%d, total=%d, pctUsed=%d.",
                                valUsedBytes,
                                valFreeBytes,
                                valTotalBytes,
                                valPercentUsed
                        )
                );
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetMemoryUsage'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }

}

/** Example (toString()-rendered) output of this Hawk MicroAgent Method:

javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(
    name=GetMemoryUsage,
    items=(
        (itemName=FreeBytes,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),
        (itemName=PercentUsed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),
        (itemName=TotalBytes,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),
        (itemName=UsedBytes,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)))
    ),
    contents={FreeBytes=50141632, PercentUsed=83, TotalBytes=308805632, UsedBytes=258664000}
)

 */

/*
Method: GetMemoryUsage

Timeout(millisecs): 10000

Description: Gets engine memory usage information.

Type: Open, Synchronous, IMPACT_INFO

	Arguments: None

	Returns:
		name: return
		type: COM.TIBCO.hawk.talon.CompositeData
		description: None
		isOpen: true
		elements:
			name: TotalBytes
			type: java.lang.Long
			description: Total number of bytes allocated to the process (free+used)
			isOpen: true
			name: FreeBytes
			type: java.lang.Long
			description: Total number of available bytes
			isOpen: true
			name: UsedBytes
			type: java.lang.Long
			description: Total number of bytes in use
			isOpen: true
			name: PercentUsed
			type: java.lang.Long
			description: Percentage of total bytes that are in use.
			isOpen: true
 */