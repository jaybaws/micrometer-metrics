package com.tibco.psg.metrics.bw.workers;
import com.tibco.psg.metrics.bw.BW5MicrometerAgent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetActiveProcessCountWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(BW5MicrometerAgent.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;

    private AtomicInteger activeProcessCount = Metrics.gauge("bwengine.activeprocess.count", Arrays.asList(Tag.of("method", "GetActiveProcessCount")), new AtomicInteger(-1));

    public GetActiveProcessCountWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            Integer valActiveProcessCount = (Integer) mbsc.invoke(objectName, "GetActiveProcessCount", null, null);

            if (valActiveProcessCount != null) {
                activeProcessCount.set(valActiveProcessCount);

                LOGGER.fine(
                        String.format(
                                "[GetActiveProcessCount] count=%d.",
                                valActiveProcessCount
                        )
                );
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetActiveProcessCount'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
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