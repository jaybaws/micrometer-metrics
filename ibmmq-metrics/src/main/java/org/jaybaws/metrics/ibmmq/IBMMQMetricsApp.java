package org.jaybaws.metrics.ibmmq;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class IBMMQMetricsApp {

    private static final int c_executorService_corePoolSize = 10;

    private static final String c_jvm_arg_prefix = IBMMQMetricsApp.class.getPackage().getName();

    private static final String c_jvm_arg_ibmmq_qmgr = c_jvm_arg_prefix + ".qmgr";
    private static final String c_jvm_arg_ibmmq_host = c_jvm_arg_prefix + ".host";
    private static final String c_jvm_arg_ibmmq_port = c_jvm_arg_prefix + ".port";
    private static final String c_jvm_arg_ibmmq_chan = c_jvm_arg_prefix + ".chan";
    private static final String c_jvm_arg_ibmmq_user = c_jvm_arg_prefix + ".user";
    private static final String c_jvm_arg_ibmmq_pass = c_jvm_arg_prefix + ".pass";
    private static final String c_jvm_arg_ibmmq_ciph = c_jvm_arg_prefix + ".ciph";
    private static final String c_jvm_arg_ibmmq_csp = c_jvm_arg_prefix + ".csp";

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(c_executorService_corePoolSize);

    public static void main(String[] args) {
        /*
         * docker run -d --env MQ_DEV=true --env MQ_QMGR_NAME=QMGR --env LICENSE=accept -p 14140:1414 ibmcom/mq:latest
         */
        LOGGER.info("Starting IBMMQMetricsApp application...");

        String qmgr = System.getProperty(c_jvm_arg_ibmmq_qmgr, "QMGR");
        String host = System.getProperty(c_jvm_arg_ibmmq_host, "localhost");
        int port = Integer.parseInt(System.getProperty(c_jvm_arg_ibmmq_port, "14140"));
        String chan = System.getProperty(c_jvm_arg_ibmmq_chan, "DEV.ADMIN.SVRCONN");
        String user = System.getProperty(c_jvm_arg_ibmmq_user, "admin");
        String pass = System.getProperty(c_jvm_arg_ibmmq_pass, "passw0rd");
        String ciph = System.getProperty(c_jvm_arg_ibmmq_ciph, null);

        boolean csp = Boolean.parseBoolean(System.getProperty(c_jvm_arg_ibmmq_csp, "false"));

        Worker worker = new Worker(qmgr, host, port, chan, user, pass, ciph, csp);

        executorService.scheduleWithFixedDelay(
                worker,
                0,
                60,
                TimeUnit.SECONDS
        );

        LOGGER.info("Worker scheduled!");
    }
}