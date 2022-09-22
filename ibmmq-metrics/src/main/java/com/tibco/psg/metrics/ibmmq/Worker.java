package com.tibco.psg.metrics.ibmmq;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final Hashtable connectionProperties = new Hashtable<String, Object>();

    private final Map<String, AtomicLong> metrics = new HashMap<String, AtomicLong>();
    private final String qmgrName;

    public Worker(String qmgr, String host, int port, String chan, String user, String pass) {
        qmgrName = qmgr;

        connectionProperties.put(CMQC.HOST_NAME_PROPERTY, host);
        connectionProperties.put(CMQC.PORT_PROPERTY, port);
        connectionProperties.put(CMQC.CHANNEL_PROPERTY, chan);
        connectionProperties.put(CMQC.USER_ID_PROPERTY, user);
        connectionProperties.put(CMQC.PASSWORD_PROPERTY, pass);
        connectionProperties.put(CMQC.USE_MQCSP_AUTHENTICATION_PROPERTY, true);

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
        try {
            /**
             * @TODO!
             *
             * sauces:
             * - https://stackoverflow.com/questions/63898748/ibm-mq-pcf-using-to-get-subscriber-count-with-a-particular-topic
             * - https://www.capitalware.com/rl_blog/?p=5463
             */

            MQQueueManager qMgr = new MQQueueManager(qmgrName, connectionProperties);
            PCFMessageAgent agent = new PCFMessageAgent(qMgr);

            doServer(qMgr);
            doQueues(agent);
            doChannels(agent);
            doSubscriptions(agent);

            agent.disconnect();
            qMgr.disconnect();
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Something went wrong during the worker-run!", t);
        }
    }

    private void doServer(MQQueueManager qmgr) {
        trackMetric("qmgr", "is_connected", "").set( (qmgr.isConnected()) ? 1 : 0 );
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

                    trackMetric("qlocal", "depth", name).set(q_depth);
                    trackMetric("qlocal", "open_input_count", name).set(q_open_input_count);
                    trackMetric("qlocal", "open_output_count", name).set(q_open_output_count);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error occurred while tracking IBM MQ queue metrics.", t);
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
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error occurred while tracking IBM MQ queue metrics.", t);
        }
    }

    private void doSubscriptions(PCFMessageAgent agent) {
        // @TODO!
    }

}