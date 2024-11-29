package org.jaybaws.metrics.bw.workers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.jaybaws.metrics.bw.util.Logger;

public class GetActiveProcessCountWorker implements Runnable {

    private final MBeanServerConnection mbsc;
    private final ObjectName objectName;

    private long valActiveProcessCount = -1;

    public GetActiveProcessCountWorker(OpenTelemetry sdk, MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;

        Meter meter = sdk.getMeter("com.tibco.bw.hawkmethod.getactiveprocesscount");
        meter
                .upDownCounterBuilder("bwengine.activeprocess.count")
                .setDescription("Reports the amount of active processes within the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valActiveProcessCount, Attributes.empty())
                );
    }

    @Override
    public void run() {
        Logger.entering(this.getClass().getCanonicalName(), "run");

        try {
            Integer value =
                    (Integer) mbsc.invoke(
                            objectName,
                            "GetActiveProcessCount",
                            null,
                            null
                    );

            if (value != null) {
                this.valActiveProcessCount = value;

                Logger.fine(
                        String.format(
                                "[GetActiveProcessCount] count=%d.",
                                valActiveProcessCount
                        )
                );
            }
        } catch (Throwable t) {
            Logger.warning("Exception invoking 'GetActiveProcessCount'...", t);
        }

        Logger.exiting(this.getClass().getCanonicalName(), "run");
    }
}

/*
Method: GetActiveProcessCount

Timeout(millisecs): 10000

Description: Gets total number of active, not paged, processes

Type: Open, Synchronous, IMPACT_INFO

	Arguments: None

	Returns:
		name: return
		type: COM.TIBCO.hawk.talon.CompositeData
		description: None
		isOpen: true
		elements:
			name: TotalActiveProcesses
			type: java.lang.Integer
			description: Number of active, not paged, processes
			isOpen: true
 */