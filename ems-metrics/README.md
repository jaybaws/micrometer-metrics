# EMS metrics

Bridges useful EMS metrics (server, queues, topics, durables) as OpenTelemetry metrics.

## How it works
This is a standalone java application that will connect to EMS using the admin Java API. Every minute, it will update its 
metrics. When this application is instrumented with an agent that is capable of shipping OpenTelemetry metrics, you can easily
set up and end-to-end metric feed.

## Available metrics
t.b.d.

## Generic tags

By default, all engines are enriched by tags to indicates to EMS server:
- `url`: the URL used to connect
- `item` the name of resource (queue, topic, durable) to which this metric belongs. Left empty for server metrics.

## Configuration options
The following JVM arguments can be passed into the application:

`org.jaybaws.metrics.ems.user`: specify the (admin) user to connect with. Defaults to `admin`.

`org.jaybaws.metrics.ems.password`: specify the (admin) user's password.

`org.jaybaws.metrics.ems.url`: the (direct) URL to connect to. Defaults to `tcp://localhost:7222`

`org.jaybaws.metrics.ems.serverinfo`: boolean to indicate whether or not to retrieve server info metrics. Defaults to `true`.

`org.jaybaws.metrics.ems.queueinfo`: boolean to indicate whether or not to retrieve metrics for each queue. Defaults to `true`.

`org.jaybaws.metrics.ems.topicinfo`: boolean to indicate whether or not to retrieve metrics for each topic. Defaults to `false`.

`org.jaybaws.metrics.ems.durableinfo`: boolean to indicate whether or not to retrieve metrics for each durable. Defaults to `false`.

### Configure for Azure Application Insights

1. Prepare

- Study the [Application Insights documentation](https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent)
- Install Java 8 or higher on your runtime environment.

2. Download the application insights `.jar` java-agent into any folder of your liking.

See [these instructions](https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent#download-the-jar-file) for more details.

3. Download the ems-metrics `jar`

Place the `ems-metrics-${version}.jar` file into a directory of your liking.

4. Set up the TIBCO EMS Java client

Copy the EMS (admin) client `.jar`'s into a `./lib` subfolder, relative to the location where you just 
installed the `ems-metrics-${version}.jar` file.

5. Run the application

From the directory where you installed `ems-metrics-${version}.jar`, run the following:

```
java \
  -javaagent:/home/tibco/applicationinsights-agent-3.4.0.jar \
  -Dorg.jaybaws.metrics.ems.user=${user} \
  -Dorg.jaybaws.metrics.ems.password=${password} \
  -Dorg.jaybaws.metrics.ems.url="tcp://${host}:${port}" \
  -Dapplicationinsights.role.name=ems-otel-metrics \
  -jar ./ems-metrics-${version}.jar \
```
