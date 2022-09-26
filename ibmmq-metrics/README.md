# IBM MQ metrics
Bridges useful IBM MQ metrics to Micrometer.

## How it works
This is a standalone java application that will connect to MQ using the admin Java API and PCF queries. Every minute, it will 
update its metrics. When this application is instrumented with an agent that is capable of shipping Micrometer metrics, you 
can easily set up and end-to-end metric feed.

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
- `com.tibco.psg.metrics.ibmmq.qmgr` specifies the name of the queue manager to connect to. Defaults to `QMGR`.
- `com.tibco.psg.metrics.ibmmq.host` specifies the hostname of the queue manager to connect to. Defaults to `localhost`.
- `com.tibco.psg.metrics.ibmmq.port` specifies the port of the queue manager to connect to. Defaults to `1414`.
- `com.tibco.psg.metrics.ibmmq.chan` specifies the server connect channel name. Defaults to `DEV.ADMIN.SVRCONN`.
- `com.tibco.psg.metrics.ibmmq.user` specifies the user to connect as. Defaults to `admin`. 
- `com.tibco.psg.metrics.ibmmq.pass` specifies the password to connect with. Defaults to `passw0rd`.
- `com.tibco.psg.metrics.ibmmq.ciph` specifies the SSL CipherSuite to be used.
- `com.tibco.psg.metrics.ibmmq.csp` indicates if MQ CSP authentication should be used. Defaults to `false`.

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
  -Dcom.tibco.psg.metrics.ibmmq.qmgr=MYQMGR \
  -Dcom.tibco.psg.metrics.ibmmq.host=mq.example.com \
  -Dcom.tibco.psg.metrics.ibmmq.port=1414 \
  -Dcom.tibco.psg.metrics.ibmmq.chan=MY.MONITOR.CL \
  -Dcom.tibco.psg.metrics.ibmmq.user=monitorUser \
  -Dcom.tibco.psg.metrics.ibmmq.pass=<password> \
  -Dcom.tibco.psg.metrics.ibmmq.ciph=TLS_RSA_WITH_AES_128_CBC_SHA256 \
  -Dcom.tibco.psg.metrics.ibmmq.csp=true \
  -javaagent:/path/to/applicationinsights-agent-3.4.0.jar \
  -jar /path/to/ibmmq-metrics-1.0.0.jar
```