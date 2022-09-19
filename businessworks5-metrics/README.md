# BusinessWorks 5.x metrics

Bridges useful bwengine microagent method (getactivities, getprocesstarterstatus, etc.) output to unique micrometer metrics.

## How it works
This javaagent will run in the bwengine JVM before the BusinessWorks code will run. This allows it to secretly enable JMX programmatically. This implies that - later on, when the BusinessWorks code starts - the Hawk methods will also be available as MBean methods.

> *Note 1*: JMX will be enabled by this javaagent regardless of the `JMX.Enabled` property value in the application's `.tra` file!
> *Note 2*: JMX will only be enabled locally. The remote JMX feature will not be used. However, if you feel that local JMX is not secure enough, this javaagent is not for you!

The javaagent listens for new JMX MBeans, and once it finds a BusinessWorks MBean, it will launch a number of workers that will query these MBeans every interval.

## Available metrics
GetExecInfo

GetMemoryUsage

GetProcessCount

GetActiveProcessCount

GetProcessStarters

GetProcessDefinitions

GetActivities

## Configuration options

com.tibco.psg.metrics.bw.loglevel

com.tibco.psg.metrics.bw.smartinstrument

com.tibco.psg.metrics.bw.bwengine.domain
com.tibco.psg.metrics.bw.bwengine.application
com.tibco.psg.metrics.bw.bwengine.instance

com.tibco.psg.metrics.bw.method.getexecinfo.[enabled|delay|initdelay]
com.tibco.psg.metrics.bw.method.getmemoryusage.[enabled|delay|initdelay]
com.tibco.psg.metrics.bw.method.getprocesscount.[enabled|delay|initdelay]
com.tibco.psg.metrics.bw.method.getactiveprocesscount.[enabled|delay|initdelay]
com.tibco.psg.metrics.bw.method.getprocessstarters.[enabled|delay|initdelay]
com.tibco.psg.metrics.bw.method.getprocessdefinitions.[enabled|delay|initdelay]
com.tibco.psg.metrics.bw.method.getactivities.[enabled|delay|initdelay]

### Configure for Azure Application Insights

1. Read Application Insights documentation
https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent

2. Download application insights .jar
https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent#download-the-jar-file

3. Prepare an Application Insights configuration file

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

4. Instrument your BW engine

Add this to your application's `.tra` file:

java.extended.properties="-javaagent:/path/to/businessworks5-metrics-1.0.0.jar -javaagent:/path/to/applicationinsights-agent-3.3.1.jar <whatever else you need to customize>"

java.property.applicationinsights.configuration.file=/tmp/applicationinsights.json OR 
java.property.applicationinsights.connection.string="<value>" OR
export APPLICATIONINSIGHTS_CONNECTION_STRING=<value>


## Advanced configuration
In environments with many deployed BusinessWorks applications, it may be cumbersome to implement the above steps for every instance. Instead, configure it once by setting the `JAVA_TOOL_OPTIONS` OS environment variable.

Make sure this environment variable is visible to the TRA's domain `hawk agent`. After all, this agent will start all BusinessWorks engines (if started via the TIBCO Administrator). All environment variables that are visible to the domain `hawk agent` will also be visible to all child PID's.

