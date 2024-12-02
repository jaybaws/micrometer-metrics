# IBM MQ metrics
Bridges useful IBM MQ metrics via OpenTelemetry.

## How it works
This is a standalone java application that will connect to MQ using the admin Java API and PCF queries. Every minute, it will 
update its metrics. Metrics are shipped via OpenTelemetry.

## Available metrics
- `qmgr`
    - `status`
    - `chinit_status`
    - `cmdserver_status`
    - `connection_count`
    - `ldap_connection_status` 
- `qlocal` - enriched with an `item` tag holding the name of the local queue.
  - `depth`
  - `open_input_count`
  - `open_output_count`
- `channels` - enriched with an `item` tag holding the name of the channel.
    - `bytes_sent`
    - `bytes_received`
    - `buffers_sent`
    - `buffers_received`
    - `messages`
    - `mca_status`
    - `status`
    - `substate` 
- `subscriptions` - enriched with an `item` tag holding the name of the subscription.
  - `message_count`
- `topic` - enriched with an `item` tag holding the name of the topic.
  - `pub_count`
  - `sub_count`
- `listener` - enriched with an `item` tag holding the name of the listener.
  - `status`
  - `backlog`
- `service` - enriched with an `item` tag holding the name of the service.
  - `status` 
 
## Generic tags
All metrics will generically be enrinched with the following tags:
- `qmgr` containing the name of the queue manager to which these metrics apply.

## Configuration options
- `org.jaybaws.metrics.ibmmq.qmgr` specifies the name of the queue manager to connect to. Defaults to `QMGR`.
- `org.jaybaws.metrics.ibmmq.host` specifies the hostname of the queue manager to connect to. Defaults to `localhost`.
- `org.jaybaws.metrics.ibmmq.port` specifies the port of the queue manager to connect to. Defaults to `1414`.
- `org.jaybaws.metrics.ibmmq.chan` specifies the server connect channel name. Defaults to `DEV.ADMIN.SVRCONN`.
- `org.jaybaws.metrics.ibmmq.user` specifies the user to connect as. Defaults to `admin`. 
- `org.jaybaws.metrics.ibmmq.pass` specifies the password to connect with. Defaults to `passw0rd`.
- `org.jaybaws.metrics.ibmmq.ciph` specifies the SSL CipherSuite to be used.
- `org.jaybaws.metrics.ibmmq.csp` indicates if MQ CSP authentication should be used. Defaults to `false`.

### Configure for Azure Application Insights
Please refer to the [Microsoft Application Insights](https://learn.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent) documentation.

Example:

```bash
APPLICATIONINSIGHTS_CONNECTION_STRING="<connect-string>" java \
  -Dpplicationinsights.role.name=ibmmq_metrics
  -Dcom.ibm.mq.cfg.useIBMCipherMappings=false \
  -Djavax.net.ssl.trustStore=/path/to/certstore.jks \
  -Djavax.net.ssl.trustStorePassword=<password> \
  -Djavax.net.ssl.trustStoreType=JKS \
  -Dorg.jaybaws.metrics.ibmmq.qmgr=MYQMGR \
  -Dorg.jaybaws.metrics.ibmmq.host=mq.example.com \
  -Dorg.jaybaws.metrics.ibmmq.port=1414 \
  -Dorg.jaybaws.metrics.ibmmq.chan=MY.MONITOR.CL \
  -Dorg.jaybaws.metrics.ibmmq.user=monitorUser \
  -Dorg.jaybaws.metrics.ibmmq.pass=<password> \
  -Dorg.jaybaws.metrics.ibmmq.ciph=TLS_RSA_WITH_AES_128_CBC_SHA256 \
  -Dorg.jaybaws.metrics.ibmmq.csp=true \
  -javaagent:/path/to/applicationinsights-agent-3.4.0.jar \
  -jar /path/to/ibmmq-metrics-1.0.0.jar
```