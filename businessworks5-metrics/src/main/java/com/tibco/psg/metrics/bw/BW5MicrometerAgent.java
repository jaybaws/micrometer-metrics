package com.tibco.psg.metrics.bw;
import com.tibco.psg.metrics.bw.util.SmartInstrumenter;
import com.tibco.psg.metrics.bw.workers.*;
import com.tibco.psg.metrics.bw.util.BWUtils;
import io.micrometer.core.instrument.*;
import javax.management.*;
import javax.management.relation.MBeanServerNotificationFilter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class BW5MicrometerAgent implements NotificationListener {

    private static final String c_defaultLogLevel = "INFO";
    private static final String c_jvm_arg_jmx = "Jmx.Enabled";
    private static final int c_executorService_corePoolSize = 10;

    private static final String c_jvm_arg_prefix = BW5MicrometerAgent.class.getPackage().getName();

    private static final String c_jvm_arg_logLevel = c_jvm_arg_prefix + ".logLevel";

    private static final String c_jvm_arg_bwengine_domain = c_jvm_arg_prefix + ".bwengine.domain";
    private static final String c_jvm_arg_bwengine_application = c_jvm_arg_prefix + ".bwengine.application";
    private static final String c_jvm_arg_bwengine_instance = c_jvm_arg_prefix + ".bwengine.instance";

    private static final String c_jvm_arg_smart_instrument = c_jvm_arg_prefix + ".smartinstrument";

    private static final String c_jvm_arg_method_prefix = c_jvm_arg_prefix + ".method";

    private static final Logger LOGGER = Logger.getLogger(BW5MicrometerAgent.class.getName());

    private final MBeanServerConnection server;
    private ObjectName engineHandle;
    private ScheduledExecutorService executorService;

    @SuppressWarnings("unused")
    public static void premain(String agentArgs) {
        if (BWUtils.isBusinessWorksEngine()) {
            LOGGER.info("JVM looks like a BusinessWorks engine, so we will instrument!");
            BW5MicrometerAgent bridge = new BW5MicrometerAgent();
            LOGGER.info("End of instrumentation!");
        } else {
            LOGGER.warning("JVM does not look like a BusinessWorks engine. Exiting and not instrumenting...");
        }
    }

    public BW5MicrometerAgent() {
        String level = System.getProperty(c_jvm_arg_logLevel, c_defaultLogLevel);
        LOGGER.setLevel(Level.parse(level));
        LOGGER.info(String.format("Set logLevel to '%s'.", level));

        /**
         * Allow for smart instrumentation
         */
        String smartInstrument = System.getProperty(c_jvm_arg_smart_instrument, "");
        SmartInstrumenter.prepare(smartInstrument);
        LOGGER.info(String.format("Ran smart-instrumentation for '%s'.", smartInstrument));

        /**
         * Get the MBeanServer where the bwengine's MBean will be hosted. It will be in the default one (platformMBeanServer)
         */
        server = ManagementFactory.getPlatformMBeanServer();

        /**
         * Determine the <prefix> from the JVM properties, and determine <domain>, <application>
         * and <instance> by reading the BWEngine's .tra file.
         */
        String engineAMIDisplayName = BWUtils.getAMIDisplayName();
        String[] instanceNameParts = engineAMIDisplayName.split("\\.");
        String domain = System.getProperty(c_jvm_arg_bwengine_domain, instanceNameParts[4]);
        String application = System.getProperty(c_jvm_arg_bwengine_application, instanceNameParts[5]);
        String instance = System.getProperty(c_jvm_arg_bwengine_instance, instanceNameParts[6]);

        LOGGER.info(String.format("BWEngine '%s' parsed as domain:%s, application:%s, instance:%s.", engineAMIDisplayName, domain, application, instance));

        /**
         * Set up the global (composite) Metric registry. Provide the global config generically.
         */
        Metrics.globalRegistry.config().commonTags(
                "domain", domain,
                "application", application,
                "instance", instance
        );

        /**
         * Listen to new MBean's being registered.
         *
         * The assumption here is that our javaagent runs *BEFORE* the bwengine actually loads.
         * This means, that at the time of this execution, the MBean we wish to consume is not yet loaded.
         *
         * In order to 'catch' this bwengine MBean, we register a listener on the MBeanServer, and finalize.
         * This will cause the static 'premain()' method to complete, and the JVM-startup cycle will proceed.
         * Either, more javaagents will be loaded, and finally the BWEngine (PEMain) will start.
         *
         * One final note: this javaagent can only function if the BWEngine (PEMain) actually enables JMX.
         * To assure this is the case, we inject the 'Jmx.Enabled=true' JVM argument.
         *
         */
        MBeanServerNotificationFilter filter = new MBeanServerNotificationFilter();
        filter.enableAllObjectNames();

        try {
            /**
             * Register ourselves as a listener for MBeans, so we can pick up the bwengine's HMA!
             */
            server.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, filter, null);

            /**
             * Force the bwengine to enable JMX, otherwise our plan dies in vain...
             */
            System.setProperty(c_jvm_arg_jmx, "true");
            LOGGER.info("Programmatically enabled JMX on the BWEngine that's about to start.");

        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Unable to register as an MBeanServer notification listener!", t);
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        MBeanServerNotification mbs = (MBeanServerNotification) notification;

        /**
         * We only want to react to the BWEngine (PEMain) MBean, not anything else (like Tomcat MBeans, etc.).
         */
        if (mbs.getMBeanName().getDomain().equals("com.tibco.bw")) {
            if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbs.getType())) {
                LOGGER.info("Caught the bwengine's HMA MBean [" + mbs.getMBeanName() + "]");
                engineHandle = mbs.getMBeanName();

                /**
                 * Only construct the ScheduledExecutorService when needed. Also, we may need to recreate it if
                 * the MBean has been lost and (re)found, since it will be destroyed (shut-down) when it's lost.
                 */
                executorService = Executors.newScheduledThreadPool(c_executorService_corePoolSize);

                /**
                 * Schedule all our workers!
                 */
                if (scheduleFor("getexecinfo"))
                    executorService.scheduleWithFixedDelay(
                            new GetExecInfoWorker(server, engineHandle),
                            initDelayFor("getexecinfo"),
                            delayFor("getexecinfo"),
                            TimeUnit.SECONDS
                    );

                if (scheduleFor("getmemoryusage"))
                    executorService.scheduleWithFixedDelay(
                            new GetMemoryUsageWorker(server, engineHandle),
                            initDelayFor("getmemoryusage"),
                            delayFor("getmemoryusage"),
                            TimeUnit.SECONDS
                    );

                if (scheduleFor("getprocesscount"))
                    executorService.scheduleWithFixedDelay(
                            new GetProcessCountWorker(server, engineHandle),
                            initDelayFor("getprocesscount"),
                            delayFor("getprocesscount"),
                            TimeUnit.SECONDS
                    );

                if (scheduleFor("getactiveprocesscount"))
                    executorService.scheduleWithFixedDelay(
                            new GetActiveProcessCountWorker(server, engineHandle),
                            initDelayFor("getactiveprocesscount"),
                            delayFor("getactiveprocesscount"),
                            TimeUnit.SECONDS
                    );

                if (scheduleFor("getprocessstarters"))
                    executorService.scheduleWithFixedDelay(
                            new GetProcessStartersWorker(server, engineHandle),
                            initDelayFor("getprocessstarters"),
                            delayFor("getprocessstarters"),
                            TimeUnit.SECONDS
                    );

                if (scheduleFor("getprocessdefinitions"))
                    executorService.scheduleWithFixedDelay(
                            new GetProcessDefinitionsWorker(server, engineHandle),
                            initDelayFor("getprocessdefinitions"),
                            delayFor("getprocessdefinitions"),
                            TimeUnit.SECONDS
                    );

                if (scheduleFor("getactivities"))
                    executorService.scheduleWithFixedDelay(
                            new GetActivitiesWorker(server, engineHandle),
                            initDelayFor("getactivities"),
                            delayFor("getactivities"),
                            TimeUnit.SECONDS
                    );

                LOGGER.info("Scheduled the workers!");

            } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbs.getType())) {
                LOGGER.warning("Lost the bwengine's HMA MBean [" + mbs.getMBeanName() + "]");
                if (mbs.getMBeanName() == engineHandle) {
                    engineHandle = null;
                    executorService.shutdown();
                }
            }
        }
    }

    private static boolean scheduleFor(String method) {
        return Boolean.valueOf(System.getProperty(c_jvm_arg_method_prefix + ".enabled", "true"));
    }

    private static int delayFor(String method) {
        return Integer.valueOf(System.getProperty(c_jvm_arg_method_prefix + ".delay", "60"));
    }

    private static int initDelayFor(String method) {
        String defaultValue;

        /**
         * By default, avoid having all the load at the same time.
         */
        switch (method) {
            case "getexecinfo": defaultValue = "5"; break;
            case "getmemoryusage": defaultValue = "10"; break;
            case "getprocesscount": defaultValue = "15"; break;
            case "getactiveprocesscount": defaultValue = "20"; break;
            case "getprocessstarters": defaultValue = "25"; break;
            case "getprocessdefinitions": defaultValue = "30"; break;
            case "getactivities": defaultValue = "35"; break;
            default: defaultValue = "0"; break;
        }

        return Integer.valueOf(System.getProperty(c_jvm_arg_method_prefix + ".initdelay", defaultValue));
    }

}