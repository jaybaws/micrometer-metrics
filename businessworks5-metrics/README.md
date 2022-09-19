# BusinessWorks 5.x metrics

Bridges useful bwengine microagent method (getactivities, getprocesstarterstatus, etc.) output to unique micrometer metrics.

## How it works
This javaagent will run in the bwengine JVM before the BusinessWorks code will run. This allows it to secretly enable JMX 
programmatically. This implies that - later on, when the BusinessWorks code starts - the Hawk methods will also be available as 
MBean methods.

> *Note 1*: JMX will be enabled by this javaagent regardless of the `JMX.Enabled` property value in the application's `.tra` file!

> *Note 2*: JMX will only be enabled locally. The remote JMX feature will not be used. However, if you feel that local JMX is 
not secure enough, this javaagent is not for you!

The javaagent listens for new JMX MBeans, and once it finds a BusinessWorks MBean, it will launch a number of workers that will 
query these MBeans every interval.

## Available metrics
- GetExecInfo
  - `bwengine.status` indicates the engine status (where `0` = unknown, `1` = `STOPPING`, `2` = `STANDBY`, `3` = `SUSPENDED` and `4` = `ACTIVE`).
  - `bwengine.uptime` indicates the elapsed time since engine process was started (milliseconds).
  - `bwengine.threads` indicates the number of worker threads in engine.
- GetMemoryUsage
  - `bwengine.memory.used` indicates the total number of bytes in use.
  - `bwengine.memory.used.pct` indicates percentage of total bytes that are in use.
  - `bwengine.memory.free` indicates the total number of available bytes.
  - `bwengine.memory.total` indicates the total number of bytes allocated to the process (free+used).
- GetProcessCount
  - `bwengine.process.count` indicates the number of running processes.
- GetActiveProcessCount
  - `bwengine.activeprocess.count` indicates the number of active, not paged, processes.
- GetProcessStarters (enriched with `process` and `activity` tags)
  - `bwengine.starters.created` indicates the number of processes created.
  - `bwengine.starters.completed` indicates the number of processes completed.
  - `bwengine.starters.creationrate` indicates the rate of process creation (processes/hour).
  - `bwengine.starters.duration` indicates the elapsed wall-clock time since the process starter started (milliseconds).
  - `bwengine.starters.running` indicates the number of processes currently running.
- GetProcessDefinitions (enriched with `process` tag)
  - `bwengine.processdefinition.created` indicates the number of processes created for this process definition.
  - `bwengine.processdefinition.suspended` indicates the number of times processes using this process definition have been suspended.
  - `bwengine.processdefinition.swapped` indicates the number of times swapped.
  - `bwengine.processdefinition.queued` indicates the number of times queued.
  - `bwengine.processdefinition.aborted` indicates the number of times aborted.
  - `bwengine.processdefinition.completed` indicates the number of times completed.
  - `bwengine.processdefinition.checkpointed` indicates the number of times checkpointed.
  - `bwengine.processdefinition.execution_total` indicates the total execution time of all processes completed using this process definition (milliseconds).
  - `bwengine.processdefinition.execution_avg` indicates the average execution time of all processes completed using this process definition (milliseconds).
  - `bwengine.processdefinition.elapsed_total` indicates the total elapsed time of all processes completed using this process definition (milliseconds).
  - `bwengine.processdefinition.elapsed_avg` indicates the average elapsed time of all processes completed using this process definition (milliseconds).
  - `bwengine.processdefinition.elapsed_min` indicates the minimum elapsed time of all processes completed using this process definition (milliseconds).
  - `bwengine.processdefinition.elapsed_max` indicates the maximum elapsed time of all processes completed using this process definition (milliseconds).
  - `bwengine.processdefinition.execution_min` indicates the minimum execution time of all processes completed using this process definition (milliseconds).
  - `bwengine.processdefinition.execution_max` indicates the maximum execution time of all processes completed using this process definition (milliseconds).
  - `bwengine.processdefinition.execution_recent` indicates the most recent ExecutionTime (milliseconds).
  - `bwengine.processdefinition.elapsed_recent` indicates the most recent ElapsedTime (milliseconds).
- GetActivities (enriched with `process`, `activityClass` and `activity` tags)
  - `bwengine.activity.executioncount` indicates the number of times this activity has been executed by this engine.
  - `bwengine.activity.elapsedtime` indicates the total wall-clock time used by all calls of this activity (milliseconds). Includes waiting time for Sleep, Call process, and Wait activities.
  - `bwengine.activity.executiontime` indicates the total wall-clock time used by all calls of this activity (milliseconds). Does not include waiting time for Sleep, Call process, and Wait activities.
  - `bwengine.activity.errorcount` indicates the number of times this activity has returned an error.
  - `bwengine.activity.elapsedtime_min` indicates the minimum value of ElaspedTime (milliseconds).
  - `bwengine.activity.elapsedtime_max` indicates the maximum value of ElapsedTime (milliseconds).
  - `bwengine.activity.executiontime_min` indicates the minimum value of ExecutionTime (milliseconds).
  - `bwengine.activity.executiontime_max` indicates the maximum value of ExecutionTime (milliseconds).
  - `bwengine.activity.elapsedtime_recent` indicates the most recent ElapsedTime (milliseconds).
  - `bwengine.activity.executiontime_recent` indicates the most recent ExecutionTime (milliseconds).

## Generic tags

By default, all engines are enriched by tags to indicates to which BusinessWorks engine they belong:
- `domain`: the administrative domain in which the application is deployed.
- `application` the name of the application, as visible in the TIBCO Administrator.
- `instance` the name of the application instance (process archive), as visible in the TIBCO Administrator.

## Configuration options

`com.tibco.psg.metrics.bw.loglevel`: the default log-level of the agent. Defaults to `INFO`. Configure any level that can be parsed by [JUL Level](https://docs.oracle.com/javase/8/docs/api/java/util/logging/Level.html)

`com.tibco.psg.metrics.bw.smartinstrument`: *experimental*

`com.tibco.psg.metrics.bw.bwengine.domain`: can be used to override the `domain` tag of all metrics.

`com.tibco.psg.metrics.bw.bwengine.application`: can be used to override the `application` tag of all metrics.

`com.tibco.psg.metrics.bw.bwengine.instance`: can be used to override the `instance` tag of all metrics.

`com.tibco.psg.metrics.bw.method.`<method(lowercase)>`.[`enabled`|`delay`|`initdelay`]

`com.tibco.psg.metrics.bw.method.getexecinfo.[enabled|delay|initdelay]`

`com.tibco.psg.metrics.bw.method.getmemoryusage.[enabled|delay|initdelay]`

`com.tibco.psg.metrics.bw.method.getprocesscount.[enabled|delay|initdelay]`

`com.tibco.psg.metrics.bw.method.getactiveprocesscount.[enabled|delay|initdelay]`

`com.tibco.psg.metrics.bw.method.getprocessstarters.[enabled|delay|initdelay]`

`com.tibco.psg.metrics.bw.method.getprocessdefinitions.[enabled|delay|initdelay]`

`com.tibco.psg.metrics.bw.method.getactivities.[enabled|delay|initdelay]`


### Configure for Azure Application Insights

1. Read Application Insights documentation

https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent

2. Download application insights .jar

https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent#download-the-jar-file

3. Prepare an Application Insights configuration file

```json
{
  "connectionString": "InstrumentationKey=<...>;IngestionEndpoint=<...>;LiveEndpoint=<...>",
  "role": {
    "name": "MyExampleApplication",
    "instance": "ProcessArchive_1"
  },
  "sampling": {
    "percentage": 100
  },
  "customDimensions": {
    "platform": "ESB",
    "env": "Staging",
    "site": "Amsterdam",
    "leg": "Orange"
  }
}
```

4. Instrument your BW engine

Add this to your application's `.tra` file:

`java.extended.properties="-javaagent:/path/to/businessworks5-metrics-1.0.0.jar -javaagent:/path/to/applicationinsights-agent-3.3.1.jar <whatever else you need to customize>"`

`java.property.applicationinsights.configuration.file=/tmp/applicationinsights.json` OR

`java.property.applicationinsights.connection.string="<value>"` OR

`export APPLICATIONINSIGHTS_CONNECTION_STRING=<value>`

## Advanced configuration
In environments with many deployed BusinessWorks applications, it may be cumbersome to implement the above steps for every 
instance. Instead, configure it once by setting the `JAVA_TOOL_OPTIONS` OS environment variable.

Make sure this environment variable is visible to the TRA's domain `hawk agent`. After all, this agent will start all 
BusinessWorks engines (if started via the TIBCO Administrator). All environment variables that are visible to the domain 
`hawk agent` will also be visible to all child PID's.