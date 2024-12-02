package org.jaybaws.metrics.ems;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class EMSMetricsApp {

    private static final int c_executorService_corePoolSize = 10;

    private static final String c_jvm_arg_prefix = EMSMetricsApp.class.getPackage().getName();

    private static final String c_jvm_arg_ems_url  = c_jvm_arg_prefix + ".url";
    private static final String c_jvm_arg_ems_user = c_jvm_arg_prefix + ".user";
    private static final String c_jvm_arg_ems_pass = c_jvm_arg_prefix + ".password";

    private static final String c_jvm_arg_ems_feature_serverinfo  = c_jvm_arg_prefix + ".serverinfo";
    private static final String c_jvm_arg_ems_feature_queueinfo   = c_jvm_arg_prefix + ".queueinfo";
    private static final String c_jvm_arg_ems_feature_topicinfo   = c_jvm_arg_prefix + ".topicinfo";
    private static final String c_jvm_arg_ems_feature_durableinfo = c_jvm_arg_prefix + ".durableinfo";

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(c_executorService_corePoolSize);

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--help")) {
            System.out.println(
                    String.format(
                            "Available properties:\n\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                            c_jvm_arg_ems_url,
                            c_jvm_arg_ems_user,
                            c_jvm_arg_ems_pass,
                            c_jvm_arg_ems_feature_serverinfo,
                            c_jvm_arg_ems_feature_queueinfo,
                            c_jvm_arg_ems_feature_topicinfo,
                            c_jvm_arg_ems_feature_durableinfo
                    )
            );
        } else {

            LOGGER.info("Starting EMSMetricsApp application...");

            String url = System.getProperty(c_jvm_arg_ems_url, "tcp://localhost:7222");
            String user = System.getProperty(c_jvm_arg_ems_user, "admin");
            String pass = System.getProperty(c_jvm_arg_ems_pass, "");

            boolean getServerInfo = Boolean.parseBoolean(System.getProperty(c_jvm_arg_ems_feature_serverinfo, "true"));
            boolean getQueueInfo = Boolean.parseBoolean(System.getProperty(c_jvm_arg_ems_feature_queueinfo, "true"));
            boolean getTopicInfo = Boolean.parseBoolean(System.getProperty(c_jvm_arg_ems_feature_topicinfo, "false"));
            boolean getDurableInfo = Boolean.parseBoolean(System.getProperty(c_jvm_arg_ems_feature_durableinfo, "false"));

            LOGGER.info(
                    String.format(
                            "Using url='%s', user='%s' password='%s'. serverinfo(%b) queues(%b) topics(%b) durables(%b)",
                            url,
                            user,
                            pass,
                            getServerInfo,
                            getQueueInfo,
                            getTopicInfo,
                            getDurableInfo
                    )
            );

            executorService.scheduleWithFixedDelay(
                    new Worker(url, user, pass, getServerInfo, getQueueInfo, getTopicInfo, getDurableInfo),
                    0,
                    60,
                    TimeUnit.SECONDS
            );

            LOGGER.info("Worker scheduled!");
        }
    }

}