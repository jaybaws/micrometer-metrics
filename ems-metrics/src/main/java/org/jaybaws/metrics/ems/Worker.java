package org.jaybaws.metrics.ems;
import com.tibco.tibjms.admin.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final String user;
    private final String pass;
    private final String url;

    private final boolean getServerInfo;
    private final boolean getQueuesInfo;
    private final boolean getTopicsInfo;
    private final boolean getDurablesInfo;

    private final Meter meter;

    private final Map<String, Long> metrics = new HashMap<String, Long>();

    public Worker(String url, String user, String pass, boolean getServerInfo, boolean getQueuesInfo, boolean getTopicsInfo, boolean getDurablesInfo) {
        this.url = url;
        this.user = user;
        this.pass = pass;

        OpenTelemetry sdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
        this.meter = sdk.getMeter("com.tibco.ems");

        // add the 'url' and 'host'

        this.getServerInfo = getServerInfo;
        this.getQueuesInfo = getQueuesInfo;
        this.getTopicsInfo = getTopicsInfo;
        this.getDurablesInfo = getDurablesInfo;
    }

    private void trackMetric(String category, String metric, String detail, long value) {
        String uniqueId = category + "/" + metric + "/" + detail;

        if (metrics.containsKey(uniqueId)) {
            metrics.replace(uniqueId, value);
        } else {
            metrics.put(uniqueId, value);
            this.meter
                    .gaugeBuilder(String.format("ems.%s.%s", category, metric))
                    .ofLongs()
                    .buildWithCallback(
                            result -> result.record(
                                    this.metrics.get(uniqueId),
                                    Attributes.builder()
                                            .put("item", detail)
                                            .build()
                            )
                    );
        }
    }

    @Override
    public void run() {
        try {
            TibjmsAdmin admin = new TibjmsAdmin(url, user, pass);

            ServerInfo si = admin.getInfo();
            String serverName = si.getServerName();

            if (this.getServerInfo) {
                trackMetric("server", "state", "", si.getState());
                trackMetric("server", "uptime", "", si.getUpTime());

                trackMetric("server", "message_memory", "", si.getMsgMem());
                trackMetric("server", "message_memory_pooled", "", si.getMsgMemPooled());

                trackMetric("server", "queue_count", "", si.getQueueCount());
                trackMetric("server", "topic_count", "", si.getTopicCount());
                trackMetric("server", "connection_count", "", si.getConnectionCount());
                trackMetric("server", "session_count", "", si.getSessionCount());
                trackMetric("server", "producer_count", "", si.getProducerCount());
                trackMetric("server", "consumer_count", "", si.getConsumerCount());
                trackMetric("server", "durable_count", "", si.getDurableCount());
                trackMetric("server", "route_recover_count", "", si.getRouteRecoverCount());

                trackMetric("server", "pending_message_count", "", si.getPendingMessageCount());
                trackMetric("server", "inbound_message_count", "", si.getInboundMessageCount());
                trackMetric("server", "outbound_message_count", "", si.getOutboundMessageCount());

                trackMetric("server", "disk_read_rate", "", si.getDiskReadRate());
                trackMetric("server", "disk_write_rate", "", si.getDiskWriteRate());
                trackMetric("server", "disk_read_operations_rate", "", si.getDiskReadOperationsRate());
                trackMetric("server", "disk_write_operations_rate", "", si.getDiskWriteOperationsRate());
                trackMetric("server", "inbound_message_rate", "", si.getInboundMessageRate());
                trackMetric("server", "outbound_message_rate", "", si.getOutboundMessageRate());
                trackMetric("server", "inbound_bytes_rate", "", si.getInboundBytesRate());
                trackMetric("server", "outbound_bytes_rate", "", si.getOutboundBytesRate());

                trackMetric("server", "pending_message_size", "", si.getPendingMessageSize());
                trackMetric("server", "message_pool_size", "", si.getMessagePoolSize());
                trackMetric("server", "sync_db_size", "", si.getSyncDBSize());
                trackMetric("server", "async_db_size", "", si.getAsyncDBSize());
            }

            if (this.getQueuesInfo) {
                QueueInfo[] queueInfo = admin.getQueues();
                for (QueueInfo qi : queueInfo) {
                    if (!qi.isTemporary()) {
                        String queueName = qi.getName();

                        trackMetric("queue", "receiver_count", queueName, qi.getReceiverCount());
                        trackMetric("queue", "delivered_message_count", queueName, qi.getDeliveredMessageCount());
                        trackMetric("queue", "intransit_message_count", queueName, qi.getInTransitMessageCount());

                        trackMetric("queue", "consumer_count", queueName, qi.getConsumerCount());
                        trackMetric("queue", "pending_message_count", queueName, qi.getPendingMessageCount());
                        trackMetric("queue", "pending_message_size", queueName, qi.getPendingMessageSize());
                        trackMetric("queue", "pending_persistent_message_count", queueName, qi.getPendingPersistentMessageCount());
                        trackMetric("queue", "pending_persistent_message_size", queueName, qi.getPendingPersistentMessageSize());

                        StatData in_stats = qi.getInboundStatistics();
                        if (in_stats != null) {
                            trackMetric("queue", "inbound_message_rate", queueName, in_stats.getMessageRate());
                            trackMetric("queue", "inbound_bytes_rate", queueName, in_stats.getByteRate());
                            trackMetric("queue", "inbound_total_messages", queueName, in_stats.getTotalMessages());
                            trackMetric("queue", "inbound_total_bytes", queueName, in_stats.getTotalBytes());
                        }

                        StatData out_stats = qi.getOutboundStatistics();
                        if (out_stats != null) {
                            trackMetric("queue", "outbound_message_rate", queueName, out_stats.getMessageRate());
                            trackMetric("queue", "outbound_bytes_rate", queueName, out_stats.getByteRate());
                            trackMetric("queue", "outbound_total_messages", queueName, out_stats.getTotalMessages());
                            trackMetric("queue", "outbound_total_bytes", queueName, out_stats.getTotalBytes());
                        }
                    }
                }
            }

            if (this.getTopicsInfo) {
                TopicInfo[] topicInfo = admin.getTopics();
                for (TopicInfo ti : topicInfo) {
                    if (!ti.isTemporary()) {
                        String topicName = ti.getName();

                        trackMetric("topic", "subscriber_count", topicName, ti.getSubscriberCount());
                        trackMetric("topic", "durable_count", topicName, ti.getDurableCount());
                        trackMetric("topic", "active_durable_count", topicName, ti.getActiveDurableCount());

                        trackMetric("topic", "consumer_count", topicName, ti.getConsumerCount());
                        trackMetric("topic", "pending_message_count", topicName, ti.getPendingMessageCount());
                        trackMetric("topic", "pending_message_size", topicName, ti.getPendingMessageSize());
                        trackMetric("topic", "pending_persistent_message_count", topicName, ti.getPendingPersistentMessageCount());
                        trackMetric("topic", "pending_persistent_message_size", topicName, ti.getPendingPersistentMessageSize());

                        StatData in_stats = ti.getInboundStatistics();
                        if (in_stats != null) {
                            trackMetric("topic", "inbound_message_rate", topicName, in_stats.getMessageRate());
                            trackMetric("topic", "inbound_bytes_rate", topicName, in_stats.getByteRate());
                            trackMetric("topic", "inbound_total_messages", topicName, in_stats.getTotalMessages());
                            trackMetric("topic", "inbound_total_bytes", topicName, in_stats.getTotalBytes());
                        }

                        StatData out_stats = ti.getOutboundStatistics();
                        if (out_stats != null) {
                            trackMetric("topic", "outbound_message_rate", topicName, out_stats.getMessageRate());
                            trackMetric("topic", "outbound_bytes_rate", topicName, out_stats.getByteRate());
                            trackMetric("topic", "outbound_total_messages", topicName, out_stats.getTotalMessages());
                            trackMetric("topic", "outbound_total_bytes", topicName, out_stats.getTotalBytes());
                        }
                    }
                }
            }

            if (this.getDurablesInfo) {
                DurableInfo[] durableInfo = admin.getDurables();
                for (DurableInfo di : durableInfo) {
                    String durableName = di.getDurableName();

                    trackMetric("durable", "pending_message_count", durableName, di.getPendingMessageCount());
                    trackMetric("durable", "delivered_message_count", durableName, di.getDeliveredMessageCount());
                    trackMetric("durable", "pending_message_size", durableName, di.getPendingMessageSize());
                }
            }

            admin.close();
        } catch (TibjmsAdminException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while trying to gather metrics.", e);
        }
    }

    public static String getIPAddress() {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Throwable t) {
            return "127.0.0.1";
        }
    }
}