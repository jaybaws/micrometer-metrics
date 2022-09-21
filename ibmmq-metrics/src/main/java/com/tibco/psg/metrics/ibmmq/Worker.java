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
import java.util.logging.Logger;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final String qmgr;
    private final String host;
    private final String chan;
    private final int port;
    private final String user;
    private final String pass;

    private Map<String, AtomicLong> metrics = new HashMap<String, AtomicLong>();

    public Worker(String qmgr, String host, int port, String chan, String user, String pass) {
        this.qmgr = qmgr;
        this.host = host;
        this.chan = chan;
        this.port = port;
        this.user = user;
        this.pass = pass;

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

            Hashtable properties = new Hashtable<String, Object>();
            properties.put(CMQC.HOST_NAME_PROPERTY, host);
            properties.put(CMQC.PORT_PROPERTY, port);
            properties.put(CMQC.CHANNEL_PROPERTY, chan);
            properties.put(CMQC.USER_ID_PROPERTY, user);
            properties.put(CMQC.PASSWORD_PROPERTY, pass);

            MQQueueManager qMgr = new MQQueueManager(qmgr, properties);
            boolean isConnected = qMgr.isConnected();

            trackMetric("qmgr", "is_connected", "").set( (isConnected) ? 1 : 0 );

            PCFMessageAgent agent = new PCFMessageAgent(qMgr);

            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
            request.addParameter(CMQC.MQCA_Q_NAME, "SYSTEM.CLUSTER.REPOSITORY.QUEUE");
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
            request.addParameter(CMQCFC.MQIACF_Q_ATTRS, new int [] { CMQCFC.MQIACF_ALL });

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

            agent.disconnect();
            qMgr.disconnect();

        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
}