package com.tibco.psg.metrics.bw.workers;
import com.tibco.psg.metrics.bw.BW5MicrometerAgent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetProcessStartersWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(BW5MicrometerAgent.class.getName());

    private MBeanServerConnection mbsc;
    private ObjectName objectName;
    private Map<String, AtomicLong> metrics = new HashMap<String, AtomicLong>();

    public GetProcessStartersWorker(MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
    }

    private AtomicLong metric(String processDefinitionName, String starterName, String metricName) {
        String uniqueId = processDefinitionName + "/" + starterName + "/" + metricName;

        AtomicLong m = metrics.get(uniqueId);
        if (m == null) {
            m = Metrics.gauge(
                    metricName,
                    Arrays.asList(
                            Tag.of("method", "GetProcessStarters"),
                            Tag.of("process", processDefinitionName),
                            Tag.of("activity", starterName)
                    ),
                    new AtomicLong(-1)
            );
            metrics.put(uniqueId, m);
        }

        return m;
    }

    @Override
    public void run() {
        LOGGER.entering(this.getClass().getCanonicalName(), "run");

        try {
            TabularDataSupport result = (TabularDataSupport) mbsc.invoke(objectName, "GetProcessStarters", null, null);

            if (result != null) {
                for (Object value : result.values()) {
                    CompositeDataSupport resultItem = (CompositeDataSupport) value;

                    String processDefinition = (String) resultItem.get("ProcessDef");

                    String starterName = (String) resultItem.get("Name");
                    String status = (String) resultItem.get("Status");

                    long valCompleted = (Integer) resultItem.get("Completed");
                    metric(processDefinition, starterName, "bwengine.starters.completed").set(valCompleted);

                    long valCreated = (Integer) resultItem.get("Created");
                    metric(processDefinition, starterName, "bwengine.starters.created").set(valCreated);

                    long valCreationRate = (Integer) resultItem.get("CreationRate");
                    metric(processDefinition, starterName, "bwengine.starters.creationrate").set(valCreationRate);

                    long valDuration = (Long) resultItem.get("Duration");
                    metric(processDefinition, starterName, "bwengine.starters.duration").set(valDuration);

                    long valRunning = (Integer) resultItem.get("Running");
                    metric(processDefinition, starterName, "bwengine.starters.running").set(valRunning);

                    long valStatus;
                    switch (status) {
                        case "INACTIVE":
                            valStatus = 0;
                            break;
                        case "FLOW-CONTROLLED":
                            valStatus = 1;
                            break;
                        case "ACTIVE":
                            valStatus = 2;
                            break;
                        default:
                            valStatus = -1;
                            break;
                    }
                    metric(processDefinition, starterName, "bwengine.starters.status").set(valStatus);

                    LOGGER.fine(
                            String.format(
                                    "[GetProcessStarters] completed=%d, created=%d, rate=%d, duration=%d, running=%d.",
                                    valCompleted,
                                    valCreated,
                                    valCreationRate,
                                    valDuration,
                                    valRunning
                            )
                    );
                }

            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Exception invoking 'GetProcessStarters'...", t);
        }

        LOGGER.exiting(this.getClass().getCanonicalName(), "run");
    }

}

/*

[AdapterCommon/Processes/Retrieve Resources.process] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=11034, Name=HTTP Receiver, ProcessDef=AdapterCommon/Processes/Retrieve Resources.process, Running=0, Start time=Jun 24, 2021 10:53:19 PM, Status=ACTIVE, Tracing=false})

[Services/FlyingBlue/FlyingBlue.V1.0.serviceagent] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=12492, Name=, ProcessDef=Services/FlyingBlue/FlyingBlue.V1.0.serviceagent, Running=0, Start time=Jun 24, 2021 10:53:17 PM, Status=ACTIVE, Tracing=false})

[Services/FlightMapper/FlightMapper.V1.0.serviceagent] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=12488, Name=, ProcessDef=Services/FlightMapper/FlightMapper.V1.0.serviceagent, Running=0, Start time=Jun 24, 2021 10:53:17 PM, Status=ACTIVE, Tracing=false})

[Services/KLMLogon/KLMLogon.serviceagent] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=12499, Name=, ProcessDef=Services/KLMLogon/KLMLogon.serviceagent, Running=0, Start time=Jun 24, 2021 10:53:17 PM, Status=ACTIVE, Tracing=false})

[builtinResource.serviceagent] = javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=GetProcessStarters,items=((itemName=Checkpointed Start,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)),(itemName=Completed,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Created,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=CreationRate,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Duration,itemType=javax.management.openmbean.SimpleType(name=java.lang.Long)),(itemName=Name,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=ProcessDef,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Running,itemType=javax.management.openmbean.SimpleType(name=java.lang.Integer)),(itemName=Start time,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Status,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=Tracing,itemType=javax.management.openmbean.SimpleType(name=java.lang.Boolean)))),contents={Checkpointed Start=false, Completed=0, Created=0, CreationRate=0, Duration=12467, Name=, ProcessDef=builtinResource.serviceagent, Running=0, Start time=Jun 24, 2021 10:53:17 PM, Status=ACTIVE, Tracing=false})

 */

/*
Method: GetProcessStarters

Timeout(millisecs): 10000

Description: Gets information about either active or inactive job creators or both

Type: Open, Synchronous, IMPACT_INFO

	Arguments:
		name: ActiveOrInactive
		type: java.lang.String
		description: Specify Active to get info about active job creators, or Inactive to get info about inactive job creators.
		isOpen: true



	Returns:
		name: return
		type: COM.TIBCO.hawk.talon.TabularData
		description: None
		isOpen: true
		elements:
			name: ProcessDef
			type: java.lang.String
			description: Process definition that contains this process starter
			isOpen: true
			name: Name
			type: java.lang.String
			description: Process starter name
			isOpen: true
			name: Status
			type: java.lang.String
			description: Status: INACTIVE, ACTIVE, or FLOW-CONTROLLED
			isOpen: true
			name: Created
			type: java.lang.Integer
			description: Number of processes created
			isOpen: true
			name: CreationRate
			type: java.lang.Integer
			description: Rate of process creation (processes/hour)
			isOpen: true
			name: Running
			type: java.lang.Integer
			description: Number of processes currently running
			isOpen: true
			name: Completed
			type: java.lang.Integer
			description: Number of processes completed
			isOpen: true
			name: Start time
			type: java.lang.String
			description: Time at which the process starter started (milliseconds)
			isOpen: true
			name: Duration
			type: java.lang.Long
			description: Elapsed wall-clock time since the process starter started (milliseconds)
			isOpen: true
			name: Checkpointed Start
			type: java.lang.Boolean
			description: True if checkpoint and confirm is enabled
			isOpen: true
			name: Tracing
			type: java.lang.Boolean
			description: True if tracing is enabled for process starter
			isOpen: true
		columns:
		columns: [ProcessDef, Name, Status, Created, CreationRate, Running, Completed, Start time, Duration, Checkpointed Start, Tracing]
		indexColumns: [ProcessDef]
 */
