package com.tibco.psg.metrics.ibmmq;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.msg.client.wmq.compat.base.internal.MQC;
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
            /**
             * @TODO!
             *
             * sauces:
             * - https://stackoverflow.com/questions/63898748/ibm-mq-pcf-using-to-get-subscriber-count-with-a-particular-topic
             */
            //Create a Hashtable with required properties
            Hashtable properties = new Hashtable<String, Object>();
            properties.put("hostname", host);
            properties.put("port", port);
            properties.put("channel", chan);

            final String queueName = "sampleQueue";

            MQQueueManager qMgr = new MQQueueManager(qmgr, properties);
            MQQueue queue = qMgr.accessQueue(queueName, MQC.MQOO_INQUIRE);

            int depth = queue.getCurrentDepth();
            int inputCount = queue.getOpenInputCount();
            int outputCount = queue.getOpenOutputCount();

            LOGGER.info(
                    String.format(
                            "Queue '%s' has %d messages, and %d receivers and %d senders.",
                            queueName,
                            depth,
                            inputCount,
                            outputCount
                    )
            );

            queue.close();
            qMgr.disconnect();

        } catch (MQException e) {
            e.printStackTrace();
        }

    }
}