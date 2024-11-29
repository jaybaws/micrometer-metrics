package org.jaybaws.metrics.bw.workers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import org.jaybaws.metrics.bw.util.Logger;

public class GetMemoryUsageWorker implements Runnable {

    private final MBeanServerConnection mbsc;
    private final ObjectName objectName;

    private long valUsedBytes = -1;
    private long valPercentUsed = -1;
    private long valFreeBytes = -1;
    private long valTotalBytes = -1;

    public GetMemoryUsageWorker(OpenTelemetry sdk, MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;

        Meter meter = sdk
                .getMeter("com.tibco.bw.hawkmethod.getmemoryusage");

        meter
                .gaugeBuilder("bwengine.memory.used")
                .setUnit("bytes")
                .setDescription("The amount of memory used by the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valUsedBytes, Attributes.empty())
                );

        meter
                .gaugeBuilder("bwengine.memory.used.pct")
                .setDescription("The percentage of availble memory that is used by the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valPercentUsed, Attributes.empty())
                );

        meter
                .gaugeBuilder("bwengine.memory.free")
                .setUnit("bytes")
                .setDescription("The amount of memory that is still free in the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valFreeBytes, Attributes.empty())
                );

        meter
                .gaugeBuilder("bwengine.memory.total")
                .setUnit("bytes")
                .setDescription("The total amount of memory allocated to the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valTotalBytes, Attributes.empty())
                );
    }

    @Override
    public void run() {
        Logger.entering(this.getClass().getCanonicalName(), "run");

        try {
            CompositeDataSupport result =
                    (CompositeDataSupport) mbsc.invoke(
                            objectName,
                            "GetMemoryUsage",
                            null,
                            null
                    );

            if (result != null) {
                this.valUsedBytes = (Long) result.get("UsedBytes");
                this.valPercentUsed = (Long) result.get("PercentUsed");
                this.valFreeBytes = (Long) result.get("FreeBytes");
                this.valTotalBytes = (Long) result.get("TotalBytes");

                Logger.fine(
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
            Logger.warning("Exception invoking 'GetMemoryUsage'...", t);
        }

        Logger.exiting(this.getClass().getCanonicalName(), "run");
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