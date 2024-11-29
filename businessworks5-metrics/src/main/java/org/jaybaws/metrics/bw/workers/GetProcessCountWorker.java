package org.jaybaws.metrics.bw.workers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.jaybaws.metrics.bw.util.Logger;

public class GetProcessCountWorker implements Runnable {

    private final MBeanServerConnection mbsc;
    private final ObjectName objectName;

    private long valProcessCount = -1;

    public GetProcessCountWorker(OpenTelemetry sdk, MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;

        Meter meter = sdk
                .getMeter("com.tibco.bw.hawkmethod.getprocesscount");

        meter
                .upDownCounterBuilder("bwengine.process.count")
                .setDescription("The total amount of process loaded in the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valProcessCount, Attributes.empty())
                );
    }

    @Override
    public void run() {
        Logger.entering(this.getClass().getCanonicalName(), "run");

        try {
            Integer value = (Integer) mbsc.invoke(objectName, "GetProcessCount", null, null);

            if (value != null) {
                this.valProcessCount = value;

                Logger.fine(
                        String.format(
                                "[GetProcessCount] count=%d.",
                                this.valProcessCount
                        )
                );
            }
        } catch (Throwable t) {
            Logger.warning("Exception invoking 'GetProcessCount'...", t);
        }

        Logger.exiting(this.getClass().getCanonicalName(), "run");
    }
}

/*
Method: GetProcessCount

Timeout(millisecs): 10000

Description: Gets total number of running processes and total number of queued processes

Type: Open, Synchronous, IMPACT_INFO

	Arguments: None

	Returns:
		name: return
		type: COM.TIBCO.hawk.talon.CompositeData
		description: None
		isOpen: true
		elements:
			name: TotalRunningProcesses
			type: java.lang.Integer
			description: Number of running processes
			isOpen: true
 */