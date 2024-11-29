package org.jaybaws.metrics.bw.workers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;
import java.util.HashMap;
import java.util.Map;
import org.jaybaws.metrics.bw.util.Logger;

public class GetProcessStartersWorker implements Runnable {

    private final MBeanServerConnection mbsc;
    private final ObjectName objectName;

    private final Meter meter;

    private final Map<String, Long> metrics = new HashMap<String, Long>();

    public GetProcessStartersWorker(OpenTelemetry sdk, MBeanServerConnection mbsc, ObjectName objectName) {
        this.mbsc = mbsc;
        this.objectName = objectName;
        this.meter = sdk.getMeter("com.tibco.bw.hawkmethod.getprocessstarters");
    }

    private void trackMetric(String metricName, String processDefinitionName, String starterName, long value) {
        String uniqueId = processDefinitionName + "/" + starterName + "/" + metricName;

        if (metrics.containsKey(uniqueId)) {
            metrics.replace(uniqueId, value);
        } else {
            metrics.put(uniqueId, value);
            this.meter
                    .gaugeBuilder(metricName)
                    .ofLongs()
                    .buildWithCallback(
                            result -> result.record(
                                    this.metrics.get(uniqueId),
                                    Attributes.builder()
                                            .put("process", processDefinitionName)
                                            .put("activity", starterName)
                                            .build()
                            )
                    );
        }
    }

    @Override
    public void run() {
        Logger.entering(this.getClass().getCanonicalName(), "run");

        try {
            TabularDataSupport result =
                    (TabularDataSupport) mbsc.invoke(
                            objectName,
                            "GetProcessStarters",
                            null,
                            null
                    );

            if (result != null) {
                for (Object value : result.values()) {
                    CompositeDataSupport resultItem = (CompositeDataSupport) value;

                    String processDefinition = (String) resultItem.get("ProcessDef");

                    String starterName = (String) resultItem.get("Name");
                    String status = (String) resultItem.get("Status");

                    long valCompleted = (Integer) resultItem.get("Completed");
                    trackMetric("bwengine.starters.completed", processDefinition, starterName, valCompleted);

                    long valCreated = (Integer) resultItem.get("Created");
                    trackMetric("bwengine.starters.created", processDefinition, starterName, valCreated);

                    long valCreationRate = (Integer) resultItem.get("CreationRate");
                    trackMetric("bwengine.starters.creationrate", processDefinition, starterName, valCreationRate);

                    long valDuration = (Long) resultItem.get("Duration");
                    trackMetric("bwengine.starters.duration", processDefinition, starterName, valDuration);

                    long valRunning = (Integer) resultItem.get("Running");
                    trackMetric("bwengine.starters.running", processDefinition, starterName, valRunning);

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
                    trackMetric("bwengine.starters.status", processDefinition, starterName, valStatus);

                    Logger.fine(
                            String.format(
                                    "[GetProcessStarters] '%s/%s' completed=%d, created=%d, rate=%d, duration=%d, running=%d.",
                                    processDefinition,
                                    starterName,
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
            Logger.warning("Exception invoking 'GetProcessStarters'...", t);
        }

        Logger.exiting(this.getClass().getCanonicalName(), "run");
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
