package org.jaybaws.metrics.bw.workers;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import org.jaybaws.metrics.bw.util.Logger;

public class GetExecInfoWorker implements Runnable {

    private final MBeanServerConnection mbsc;
    private final ObjectName objectName;

    private long valStatus = -1;
    private long valUptime = -1;
    private long valThreads = -1;

    public GetExecInfoWorker(OpenTelemetry sdk, MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;

        Meter meter = sdk.getMeter("com.tibco.bw.hawkmethod.getexecinfo");

        meter
                .gaugeBuilder("bwengine.status")
                .ofLongs()
                .setDescription("Reports the status of the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valStatus, Attributes.empty())
                );

        meter
                .gaugeBuilder("bwengine.uptime")
                .ofLongs()
                .setDescription("Reports the uptime of the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valUptime, Attributes.empty())
                );

        meter
                .gaugeBuilder("bwengine.threads")
                .ofLongs()
                .setDescription("Reports the amount of availble engine threads within the BW engine.")
                .buildWithCallback(
                        result -> result.record(
                                this.valThreads, Attributes.empty())
                );
    }

    @Override
    public void run() {
        Logger.entering(this.getClass().getCanonicalName(), "run");

        try {
            CompositeDataSupport result =
                    (CompositeDataSupport) mbsc.invoke(
                            objectName,
                            "GetExecInfo",
                            null,
                            null
                    );

            if (result != null) {
                this.valUptime = (Long) result.get("Uptime");
                this.valThreads = (Integer) result.get("Threads");
                String status = (String) result.get("Status");

                switch (status) {
                    case "ACTIVE":
                        this.valStatus = 4;
                        break;
                    case "SUSPENDED":
                        this.valStatus = 3;
                        break;
                    case "STANDBY":
                        this.valStatus = 2;
                        break;
                    case "STOPPING":
                        this.valStatus = 1;
                        break;
                    default:
                        this.valStatus = 0;
                        break;
                }

                Logger.fine(
                        String.format(
                                "[GetExecInfo] status=%s/%d, uptime=%d, threads=%d.",
                                status,
                                this.valStatus,
                                this.valUptime,
                                this.valThreads                        )
                );
            }
        } catch (Throwable t) {
            Logger.warning("Exception invoking 'GetExecInfo'...", t);
        }

        Logger.exiting(this.getClass().getCanonicalName(), "run");
    }
}

/*
Method: GetExecInfo

Timeout(millisecs): 10000

Description: Gets process engine execution information

Type: Open, Synchronous, IMPACT_INFO

	Arguments: None

	Returns:
		name: return
		type: COM.TIBCO.hawk.talon.CompositeData
		description: None
		isOpen: true
		elements:
			name: Status
			type: java.lang.String
			description: Engine status (ACTIVE, SUSPENDED, STANDBY or STOPPING)
			isOpen: true
			name: Uptime
			type: java.lang.Long
			description: Elapsed time since engine process was started (milliseconds)
			isOpen: true
			name: Threads
			type: java.lang.Integer
			description: Number of worker threads in engine.
			isOpen: true
			name: Version
			type: java.lang.String
			description: Repo configuration version
			isOpen: true
 */