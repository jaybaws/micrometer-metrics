package com.tibco.psg.metrics.bw.workers;

import com.tibco.psg.metrics.bw.BW5MicrometerAgent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetExecInfoWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(BW5MicrometerAgent.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    private AtomicLong metricStatus  = Metrics.gauge("bwengine.status",  Arrays.asList(Tag.of("method", "GetExecInfo")), new AtomicLong(-1));
    private AtomicLong metricUptime  = Metrics.gauge("bwengine.uptime",  Arrays.asList(Tag.of("method", "GetExecInfo")), new AtomicLong(-1));
    private AtomicLong metricThreads = Metrics.gauge("bwengine.threads", Arrays.asList(Tag.of("method", "GetExecInfo")), new AtomicLong(-1));

    public GetExecInfoWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            CompositeDataSupport result = (CompositeDataSupport) mbsc.invoke(objectName, "GetExecInfo", null, null);

            if (result != null) {

                String status = (String) result.get("Status");

                long valUptime = (Long) result.get("Uptime");
                long valThreads = (Integer) result.get("Threads");
                long valStatus;
                switch (status) {
                    case "ACTIVE":
                        valStatus = 4;
                        break;
                    case "SUSPENDED":
                        valStatus = 3;
                        break;
                    case "STANDBY":
                        valStatus = 2;
                        break;
                    case "STOPPING":
                        valStatus = 1;
                        break;
                    default:
                        valStatus = 0;
                        break;
                }

                metricStatus.set(valStatus);
                metricUptime.set(valUptime);
                metricThreads.set(valThreads);

                LOGGER.fine(
                        String.format(
                                "[GetExecInfo] status=%s/%d, uptime=%d, threads=%d.",
                                status,
                                valStatus,
                                valUptime,
                                valThreads                        )
                );
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetExecInfo'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
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