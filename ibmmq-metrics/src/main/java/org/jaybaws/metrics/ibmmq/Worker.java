package org.jaybaws.metrics.ibmmq;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final Hashtable connectionProperties = new Hashtable<String, Object>();

    private final Map<String, AtomicLong> metrics = new HashMap<String, AtomicLong>();
    private final String qmgrName;

    public Worker(String qmgr, String host, int port, String chan, String user, String pass, String sslCiph, boolean useMQCSP) {
        qmgrName = qmgr;

        connectionProperties.put(CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_CLIENT);
        connectionProperties.put(CMQC.APPNAME_PROPERTY, "ibmmq-metrics");
        connectionProperties.put(CMQC.CHANNEL_PROPERTY, chan);

        connectionProperties.put(CMQC.HOST_NAME_PROPERTY, host);
        connectionProperties.put(CMQC.PORT_PROPERTY, port);

        connectionProperties.put(CMQC.USE_MQCSP_AUTHENTICATION_PROPERTY, useMQCSP);
        connectionProperties.put(CMQC.USER_ID_PROPERTY, user);
        connectionProperties.put(CMQC.PASSWORD_PROPERTY, pass);

        if (sslCiph != null) {
            connectionProperties.put(CMQC.SSL_CIPHER_SUITE_PROPERTY, sslCiph);
        }

        /**
         * Set up the global (composite) Metric registry. Provide the global config generically.
         */
        Metrics.globalRegistry.config().commonTags(
                "qmgr", qmgr
        );
    }

    private AtomicLong trackMetric(String category, String metric, String detail) {
        String uniqueId = category + "/" + metric + "/" + detail;

        AtomicLong m = metrics.get(uniqueId);
        if (m == null) {
            m = Metrics.gauge(
                    String.format("ibmmq.%s.%s", category, metric),
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

        boolean succeeded = false;

        try {
            MQQueueManager qMgr = new MQQueueManager(qmgrName, connectionProperties);
            PCFMessageAgent agent = new PCFMessageAgent(qMgr);

            doServer(qMgr, agent);
            doChannels(agent);
            doQueues(agent);
            doTopics(agent);
            doSubscriptions(agent);
            doListeners(agent);
            doServices(agent);

            agent.disconnect();
            qMgr.disconnect();

            succeeded = true;
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Something went wrong during the worker-run!", t);
        } finally {
            trackMetric("qmgr", "available", "").set( (succeeded) ? 1 : 0 );
        }
    }

    private void doServer(MQQueueManager qmgr, PCFMessageAgent agent) {
        trackMetric("qmgr", "is_connected", "").set( (qmgr.isConnected()) ? 1 : 0 );

        try {
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS);
            request.addParameter(CMQCFC.MQIACF_Q_MGR_STATUS_ATTRS, new int[]{CMQCFC.MQIACF_ALL});
            PCFMessage[] responses = agent.send(request);

            for (PCFMessage response : responses) {
                if ((response.getCompCode() == CMQC.MQCC_OK) && (response.getParameterValue(CMQC.MQCA_Q_MGR_NAME) != null) ) {
                    String name = response.getStringParameterValue(CMQC.MQCA_Q_MGR_NAME);
                    if (name != null)
                        name = name.trim();

                    int status = response.getIntParameterValue(CMQCFC.MQIACF_Q_MGR_STATUS);
                    int chinit_status = response.getIntParameterValue(CMQCFC.MQIACF_CHINIT_STATUS);
                    int cmdserver_status = response.getIntParameterValue(CMQCFC.MQIACF_CMD_SERVER_STATUS);
                    int connection_count = response.getIntParameterValue(CMQCFC.MQIACF_CONNECTION_COUNT);
                    int ldap_connection_status = response.getIntParameterValue(CMQCFC.MQIACF_LDAP_CONNECTION_STATUS);

                    trackMetric("qmgr", "status", name).set(status);
                    trackMetric("qmgr", "chinit_status", name).set(chinit_status);
                    trackMetric("qmgr", "cmdserver_status", name).set(cmdserver_status);
                    trackMetric("qmgr", "connection_count", name).set(connection_count);
                    trackMetric("qmgr", "ldap_connection_status", name).set(ldap_connection_status);
                }
            }
        } catch (PCFException e) {
            if (e.reasonCode != 2085)
                LOGGER.log(Level.WARNING, "PCF Error occurred while tracking IBM MQ queue manager metrics.", e);
        } catch (IOException | MQDataException e) {
            LOGGER.log(Level.WARNING, "IO or Data error occurred while tracking IBM MQ queue manager metrics.", e);
        }
    }

    private void doQueues(PCFMessageAgent agent) {
        try {
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
            request.addParameter(CMQC.MQCA_Q_NAME, "*");
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
            request.addParameter(CMQCFC.MQIACF_Q_ATTRS, new int[]{CMQCFC.MQIACF_ALL});

            PCFMessage[] responses = agent.send(request);

            for (PCFMessage response : responses) {
                if ((response.getCompCode() == CMQC.MQCC_OK) && (response.getParameterValue(CMQC.MQCA_Q_NAME) != null)) {
                    String name = response.getStringParameterValue(CMQC.MQCA_Q_NAME);
                    if (name != null)
                        name = name.trim();

                    int q_depth = response.getIntParameterValue(CMQC.MQIA_CURRENT_Q_DEPTH);
                    int q_open_input_count = response.getIntParameterValue(CMQC.MQIA_OPEN_INPUT_COUNT);
                    int q_open_output_count = response.getIntParameterValue(CMQC.MQIA_OPEN_OUTPUT_COUNT);

                    int type = response.getIntParameterValue(CMQC.MQIA_DEFINITION_TYPE);

                    if (type == 1) { // This filters out all the AMQ.* crap that will blow up your registry!
                        trackMetric("qlocal", "depth", name).set(q_depth);
                        trackMetric("qlocal", "open_input_count", name).set(q_open_input_count);
                        trackMetric("qlocal", "open_output_count", name).set(q_open_output_count);

                        if (response.getParameterValue(CMQC.MQIA_MSG_DEQ_COUNT) != null) {
                            int q_dequeue_count = response.getIntParameterValue(CMQC.MQIA_MSG_DEQ_COUNT);
                            trackMetric("qlocal", "dequeued_messages", name).set(q_dequeue_count);
                        }

                        if (response.getParameterValue(CMQC.MQIA_MSG_ENQ_COUNT) != null) {
                            int q_enqueue_count = response.getIntParameterValue(CMQC.MQIA_MSG_ENQ_COUNT);
                            trackMetric("qlocal", "enqueued_messages", name).set(q_enqueue_count);
                        }
                    }
                }
            }
        } catch (PCFException e) {
            if (e.reasonCode != 2085)
                LOGGER.log(Level.WARNING, "PCF Error occurred while tracking IBM MQ queue metrics.", e);
        } catch (IOException | MQDataException e) {
            LOGGER.log(Level.WARNING, "IO or Data error occurred while tracking IBM MQ queue metrics.", e);
        }
    }

    private void doChannels(PCFMessageAgent agent) {
        try {
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_CHANNEL_STATUS);
            request.addParameter(CMQCFC.MQCACH_CHANNEL_NAME, "*");
            request.addParameter(CMQCFC.MQIACH_CHANNEL_INSTANCE_ATTRS, new int[]{CMQCFC.MQIACF_ALL});
            PCFMessage[] responses = agent.send(request);

            for (PCFMessage response : responses) {
                if ((response.getCompCode() == CMQC.MQCC_OK) && (response.getParameterValue(CMQCFC.MQCACH_CHANNEL_NAME) != null)) {
                    String name = response.getStringParameterValue(CMQCFC.MQCACH_CHANNEL_NAME);
                    if (name != null)
                        name = name.trim();

                    int channel_bytes_sent = response.getIntParameterValue(CMQCFC.MQIACH_BYTES_SENT);
                    int channel_bytes_received = response.getIntParameterValue(CMQCFC.MQIACH_BYTES_RECEIVED);
                    int channel_buffers_sent = response.getIntParameterValue(CMQCFC.MQIACH_BUFFERS_SENT);
                    int channel_buffers_received = response.getIntParameterValue(CMQCFC.MQIACH_BUFFERS_RECEIVED);
                    int channel_messages = response.getIntParameterValue(CMQCFC.MQIACH_MSGS);
                    int channel_mca_status = response.getIntParameterValue(CMQCFC.MQIACH_MCA_STATUS);
                    int channel_status = response.getIntParameterValue(CMQCFC.MQIACH_CHANNEL_STATUS);
                    int channel_substate = response.getIntParameterValue(CMQCFC.MQIACH_CHANNEL_SUBSTATE);

                    trackMetric("channels", "bytes_sent", name).set(channel_bytes_sent);
                    trackMetric("channels", "bytes_received", name).set(channel_bytes_received);
                    trackMetric("channels", "buffers_sent", name).set(channel_buffers_sent);
                    trackMetric("channels", "buffers_received", name).set(channel_buffers_received);
                    trackMetric("channels", "messages", name).set(channel_messages);
                    trackMetric("channels", "mca_status", name).set(channel_mca_status);
                    trackMetric("channels", "status", name).set(channel_status);
                    trackMetric("channels", "substate", name).set(channel_substate);
                }
            }
        } catch (PCFException e) {
            if (e.reasonCode != 2085)
                LOGGER.log(Level.WARNING, "PCF Error occurred while tracking IBM MQ channel metrics.", e);
        } catch (IOException | MQDataException e) {
            LOGGER.log(Level.WARNING, "IO or Data error occurred while tracking IBM MQ channel metrics.", e);
        }
    }

    private void doSubscriptions(PCFMessageAgent agent) {
        try {
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_SUB_STATUS);
            request.addParameter(CMQCFC.MQCACF_SUB_NAME, "*");
            request.addParameter(CMQCFC.MQIACF_SUB_STATUS_ATTRS, new int[]{CMQCFC.MQIACF_ALL});
            PCFMessage[] responses = agent.send(request);

            for (PCFMessage response : responses) {
                if ((response.getCompCode() == CMQC.MQCC_OK) && (response.getParameterValue(CMQCFC.MQCACF_SUB_NAME) != null)) {
                    String name = response.getStringParameterValue(CMQCFC.MQCACF_SUB_NAME);
                    if (name != null)
                        name = name.trim();

                    int message_count = response.getIntParameterValue(CMQCFC.MQIACF_MESSAGE_COUNT);

                    trackMetric("subscriptions", "message_count", name).set(message_count);
                }
            }
        } catch (PCFException e) {
            if (e.reasonCode != 2085)
                LOGGER.log(Level.WARNING, "PCF Error occurred while tracking IBM MQ subscriptionm metrics.", e);
        } catch (IOException | MQDataException e) {
            LOGGER.log(Level.WARNING, "IO or Data error occurred while tracking IBM MQ subscription metrics.", e);
        }
    }

    public void doTopics(PCFMessageAgent agent) {
        try {

            PCFMessage topicsRequest = new PCFMessage(CMQCFC.MQCMD_INQUIRE_TOPIC);
            topicsRequest.addParameter(CMQC.MQCA_TOPIC_NAME, "*");
            PCFMessage[] topicsResponses = agent.send(topicsRequest);

            for (PCFMessage topicResponse : topicsResponses) {
                String topicName = topicResponse.getStringParameterValue(CMQC.MQCA_TOPIC_NAME);
                String topicString = topicResponse.getStringParameterValue(CMQC.MQCA_TOPIC_STRING);

                PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_TOPIC_STATUS);
                request.addParameter(CMQC.MQCA_TOPIC_STRING, topicString);
                PCFMessage[] responses = agent.send(request);

                for (PCFMessage response : responses) {
                    if ((response.getCompCode() == CMQC.MQCC_OK) ) {
                        int pubCount = response.getIntParameterValue(CMQC.MQIA_PUB_COUNT);
                        int subCount = response.getIntParameterValue(CMQC.MQIA_SUB_COUNT);

                        trackMetric("topic", "pub_count", topicName).set(pubCount);
                        trackMetric("topic", "sub_count", topicName).set(subCount);
                    }
                }
            }
        } catch (PCFException e) {
            if (e.reasonCode != 2085)
                LOGGER.log(Level.WARNING, "PCF Error occurred while tracking IBM MQ topic metrics.", e);
        } catch (IOException | MQDataException e) {
            LOGGER.log(Level.WARNING, "IO or Data error occurred while tracking IBM MQ topic metrics.", e);
        }
    }

    public void doListeners(PCFMessageAgent agent) {
        try {
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS);
            request.addParameter(CMQCFC.MQCACH_LISTENER_NAME, "*");
            request.addParameter(CMQCFC.MQIACF_LISTENER_STATUS_ATTRS, new int[]{CMQCFC.MQIACF_ALL});
            PCFMessage[] responses = agent.send(request);

            for (PCFMessage response : responses) {
                if ((response.getCompCode() == CMQC.MQCC_OK) && (response.getParameterValue(CMQCFC.MQCACH_LISTENER_NAME) != null)) {
                    String name = response.getStringParameterValue(CMQCFC.MQCACH_LISTENER_NAME);
                    if (name != null)
                        name = name.trim();

                    int status = response.getIntParameterValue(CMQCFC.MQIACH_LISTENER_STATUS);
                    int backlog = response.getIntParameterValue(CMQCFC.MQIACH_BACKLOG);

                    trackMetric("listener", "status", name).set(status);
                    trackMetric("listener", "backlog", name).set(backlog);
                }
            }
        } catch (PCFException e) {
            if (e.reasonCode != 2085)
                LOGGER.log(Level.WARNING, "PCF Error occurred while tracking IBM MQ listener metrics.", e);
        } catch (IOException | MQDataException e) {
            LOGGER.log(Level.WARNING, "IO or Data error occurred while tracking IBM MQ listener metrics.", e);
        }
    }

    public void doServices(PCFMessageAgent agent) {
        try {
            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_SERVICE_STATUS);
            request.addParameter(CMQC.MQCA_SERVICE_NAME, "*");
            request.addParameter(CMQCFC.MQIACF_SERVICE_STATUS_ATTRS, new int[]{CMQCFC.MQIACF_ALL});
            PCFMessage[] responses = agent.send(request);

            for (PCFMessage response : responses) {
                if ((response.getCompCode() == CMQC.MQCC_OK) && (response.getParameterValue(CMQC.MQCA_SERVICE_NAME) != null)) {
                    String name = response.getStringParameterValue(CMQC.MQCA_SERVICE_NAME);
                    if (name != null)
                        name = name.trim();

                    int status = response.getIntParameterValue(CMQCFC.MQIACF_SERVICE_STATUS);

                    trackMetric("service", "status", name).set(status);
                }
            }
        } catch (PCFException e) {
            if (e.reasonCode != 2085)
                LOGGER.log(Level.WARNING, "PCF Error occurred while tracking IBM MQ service metrics.", e);
        } catch (IOException | MQDataException e) {
            LOGGER.log(Level.WARNING, "IO or Data error occurred while tracking IBM MQ service metrics.", e);
        }
    }
}