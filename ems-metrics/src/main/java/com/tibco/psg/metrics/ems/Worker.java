package com.tibco.psg.metrics.ems;
import com.tibco.tibjms.admin.*;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final String user;
    private final String pass;
    private final String url;

    private final boolean getServerInfo;
    private final boolean getQueuesInfo;
    private final boolean getTopicsInfo;
    private final boolean getDurablesInfo;

    private Map<String, AtomicLong> metrics = new HashMap<String, AtomicLong>();

    public Worker(String url, String user, String pass, boolean getServerInfo, boolean getQueuesInfo, boolean getTopicsInfo, boolean getDurablesInfo) {
        this.url = url;
        this.user = user;
        this.pass = pass;

        this.getServerInfo = getServerInfo;
        this.getQueuesInfo = getQueuesInfo;
        this.getTopicsInfo = getTopicsInfo;
        this.getDurablesInfo = getDurablesInfo;

        /**
         * Set up the global (composite) Metric registry. Provide the global config generically.
         */
        Metrics.globalRegistry.config().commonTags(
                "url", url
        );
    }

    private AtomicLong trackMetric(String category, String metric, String detail) {
        String uniqueId = category + "/" + metric + "/" + detail;

        AtomicLong m = metrics.get(uniqueId);
        if (m == null) {
            m = Metrics.gauge(
                    String.format("ems.%s.%s", category, metric),
                    Arrays.asList(
                            Tag.of("item", detail)
                    ),
                    new AtomicLong(-1)
            );
            metrics.put(uniqueId, m);
        }

        return m;
    }

    @Override
    public void run() {
        try {
            TibjmsAdmin admin = new TibjmsAdmin(url, user, pass);

            ServerInfo si = admin.getInfo();
            String serverName = si.getServerName();

            if (this.getServerInfo) {
                trackMetric("server", "state", "").set(si.getState());
                trackMetric("server", "uptime", "").set(si.getUpTime());

                trackMetric("server", "message_memory", "").set(si.getMsgMem());
                trackMetric("server", "message_memory_pooled", "").set(si.getMsgMemPooled());

                trackMetric("server", "queue_count", "").set(si.getQueueCount());
                trackMetric("server", "topic_count", "").set(si.getTopicCount());
                trackMetric("server", "connection_count", "").set(si.getConnectionCount());
                trackMetric("server", "session_count", "").set(si.getSessionCount());
                trackMetric("server", "producer_count", "").set(si.getProducerCount());
                trackMetric("server", "consumer_count", "").set(si.getConsumerCount());
                trackMetric("server", "durable_count", "").set(si.getDurableCount());
                trackMetric("server", "route_recover_count", "").set(si.getRouteRecoverCount());

                trackMetric("server", "pending_message_count", "").set(si.getPendingMessageCount());
                trackMetric("server", "inbound_message_count", "").set(si.getInboundMessageCount());
                trackMetric("server", "outbound_message_count", "").set(si.getOutboundMessageCount());

                trackMetric("server", "disk_read_rate", "").set(si.getDiskReadRate());
                trackMetric("server", "disk_write_rate", "").set(si.getDiskWriteRate());
                trackMetric("server", "disk_read_operations_rate", "").set(si.getDiskReadOperationsRate());
                trackMetric("server", "disk_write_operations_rate", "").set(si.getDiskWriteOperationsRate());
                trackMetric("server", "inbound_message_rate", "").set(si.getInboundMessageRate());
                trackMetric("server", "outbound_message_rate", "").set(si.getOutboundMessageRate());
                trackMetric("server", "inbound_bytes_rate", "").set(si.getInboundBytesRate());
                trackMetric("server", "outbound_bytes_rate", "").set(si.getOutboundBytesRate());

                trackMetric("server", "pending_message_size", "").set(si.getPendingMessageSize());
                trackMetric("server", "message_pool_size", "").set(si.getMessagePoolSize());
                trackMetric("server", "sync_db_size", "").set(si.getSyncDBSize());
                trackMetric("server", "async_db_size", "").set(si.getAsyncDBSize());
            }

            if (this.getQueuesInfo) {
                QueueInfo[] queueInfo = admin.getQueuesStatistics();
                for (QueueInfo qi : queueInfo) {
                    String queueName = qi.getName();

                    trackMetric("queue", "receiver_count", queueName).set(qi.getReceiverCount());
                    trackMetric("queue", "delivered_message_count", queueName).set(qi.getDeliveredMessageCount());
                    trackMetric("queue", "intransit_message_count", queueName).set(qi.getInTransitMessageCount());

                    trackMetric("queue", "consumer_count", queueName).set(qi.getConsumerCount());
                    trackMetric("queue", "pending_message_count", queueName).set(qi.getPendingMessageCount());
                    trackMetric("queue", "pending_message_size", queueName).set(qi.getPendingMessageSize());
                    trackMetric("queue", "pending_persistent_message_count", queueName).set(qi.getPendingPersistentMessageCount());
                    trackMetric("queue", "pending_persistent_message_size", queueName).set(qi.getPendingPersistentMessageSize());
                }
            }

            if (this.getTopicsInfo) {
                TopicInfo[] topicInfo = admin.getTopicsStatistics();
                for (TopicInfo ti : topicInfo) {
                    String topicName = ti.getName();

                    trackMetric("topic", "subscriber_count", topicName).set(ti.getSubscriberCount());
                    trackMetric("topic", "durable_count", topicName).set(ti.getDurableCount());
                    trackMetric("topic", "active_durable_count", topicName).set(ti.getActiveDurableCount());

                    trackMetric("topic", "consumer_count", topicName).set(ti.getConsumerCount());
                    trackMetric("topic", "pending_message_count", topicName).set(ti.getPendingMessageCount());
                    trackMetric("topic", "pending_message_size", topicName).set(ti.getPendingMessageSize());
                    trackMetric("topic", "pending_persistent_message_count", topicName).set(ti.getPendingPersistentMessageCount());
                    trackMetric("topic", "pending_persistent_message_size", topicName).set(ti.getPendingPersistentMessageSize());
                }
            }

            if (this.getDurablesInfo) {
                DurableInfo[] durableInfo = admin.getDurables();
                for (DurableInfo di : durableInfo) {
                    String durableName = di.getDurableName();

                    trackMetric("durable", "pending_message_count", durableName).set(di.getPendingMessageCount());
                    trackMetric("durable", "delivered_message_count", durableName).set(di.getDeliveredMessageCount());
                    trackMetric("durable", "pending_message_size", durableName).set(di.getPendingMessageSize());
                }
            }

            admin.close();
        } catch (TibjmsAdminException e) {
            LOGGER.log(Level.SEVERE, "Exception occurred while trying to gather metrics.", e);
        }
    }
}