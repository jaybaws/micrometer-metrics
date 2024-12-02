package org.jaybaws.metrics.bw;
import io.opentelemetry.api.OpenTelemetry;
import org.jaybaws.metrics.bw.metrics.JVM;
import org.jaybaws.metrics.bw.util.Constants;
import org.jaybaws.metrics.bw.workers.*;
import org.jaybaws.metrics.bw.util.BWUtils;
import javax.management.*;
import javax.management.relation.MBeanServerNotificationFilter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import org.jaybaws.metrics.bw.util.Logger;

public class BW5MonitorAgent implements NotificationListener {

    private final OpenTelemetry otelSdk;
    private final MBeanServerConnection server;
    private ObjectName engineHandle;
    private ScheduledExecutorService executorService;

    @SuppressWarnings("unused")
    public static void premain(String agentArgs) {
        if (BWUtils.isBusinessWorksEngine()) {
            Logger.info("JVM looks like a BusinessWorks engine, so we will instrument!");
            BW5MonitorAgent bridge = new BW5MonitorAgent();
            Logger.info("End of instrumentation!");
        } else {
            Logger.warning("JVM does not look like a BusinessWorks engine... Leaving it as it is!");
        }
    }

    public BW5MonitorAgent() {
        /*
         * Grab us an OpenTelemetry SDK
         */

        this.otelSdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
        /*
         * Get the MBeanServer where the bwengine's MBean will be hosted.
         * It will be in the default one (platformMBeanServer)
         */
        server = ManagementFactory.getPlatformMBeanServer();
        MBeanServerFactory.findMBeanServer(null);

        /*
         * Determine the <prefix> from the JVM properties, and determine <domain>, <application>
         * and <instance> by reading the BWEngine's .tra file.
         */
        String engineAMIDisplayName = BWUtils.getAMIDisplayName();
        String[] instanceNameParts = engineAMIDisplayName.split("\\.");
        String domain = instanceNameParts[4];
        String application = instanceNameParts[5];
        String instance = instanceNameParts[6];

        Logger.info(
                String.format(
                        "BWEngine '%s' parsed as domain:%s, application:%s, instance:%s.",
                        engineAMIDisplayName,
                        domain,
                        application,
                        instance
                )
        );

        /*
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
        try {
            /*
             * Register ourselves as a listener for MBeans, so we can pick up the bwengine's HMA!
             */
            MBeanServerNotificationFilter filter = new MBeanServerNotificationFilter();
            filter.enableAllObjectNames();

            server.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, filter, null);
            /*
             * Force the bwengine to enable JMX, otherwise our plan dies in vain...
             */
            System.setProperty("Jmx.Enabled", "true");
            Logger.info("Programmatically enabled JMX on the BWEngine that's about to start.");

        } catch (Throwable t) {
            Logger.severe("Unable to register as an MBeanServer notification listener!", t);
        }
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        MBeanServerNotification mbs = (MBeanServerNotification) notification;
        String domain = mbs.getMBeanName().getDomain();

        /*
         * We only want to react to the BWEngine (PEMain) MBean, not anything else (like Tomcat MBeans, etc.).
         */
        if (domain.equals("com.tibco.bw")) {
            if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(mbs.getType())) {
                Logger.info("Caught the bwengine's HMA MBean [" + mbs.getMBeanName() + "]");
                engineHandle = mbs.getMBeanName();

                JVM.instrument(this.otelSdk);

                /*
                 * Only construct the ScheduledExecutorService when needed. Also, we may need to recreate it if
                 * the MBean has been lost and (re)found, since it will be destroyed (shut-down) when it's lost.
                 */
                executorService = Executors.newScheduledThreadPool(Constants.EXECUTORSERVICE_CORE_POOLSIZE);

                /*
                 * Schedule all our workers!
                 */
                Logger.info("Start scheduling the workers!");
                if (scheduleFor("getexecinfo")) {
                    executorService.scheduleWithFixedDelay(
                            new GetExecInfoWorker(this.otelSdk, server, engineHandle),
                            initDelayFor("getexecinfo"),
                            delayFor("getexecinfo"),
                            TimeUnit.SECONDS
                    );
                    Logger.info("--> GetExecInfo worker scheduled.");
                }

                if (scheduleFor("getmemoryusage")) {
                    executorService.scheduleWithFixedDelay(
                            new GetMemoryUsageWorker(this.otelSdk, server, engineHandle),
                            initDelayFor("getmemoryusage"),
                            delayFor("getmemoryusage"),
                            TimeUnit.SECONDS
                    );
                    Logger.info("--> GetMemoryUsage worker scheduled.");
                }

                if (scheduleFor("getprocesscount")) {
                    executorService.scheduleWithFixedDelay(
                            new GetProcessCountWorker(this.otelSdk, server, engineHandle),
                            initDelayFor("getprocesscount"),
                            delayFor("getprocesscount"),
                            TimeUnit.SECONDS
                    );
                    Logger.info("--> GetProcessCount worker scheduled.");
                }

                if (scheduleFor("getactiveprocesscount")) {
                    executorService.scheduleWithFixedDelay(
                            new GetActiveProcessCountWorker(this.otelSdk, server, engineHandle),
                            initDelayFor("getactiveprocesscount"),
                            delayFor("getactiveprocesscount"),
                            TimeUnit.SECONDS
                    );
                    Logger.info("--> GetActiveProcessCount worker scheduled.");
                }

                if (scheduleFor("getprocessstarters")) {
                    executorService.scheduleWithFixedDelay(
                            new GetProcessStartersWorker(this.otelSdk, server, engineHandle),
                            initDelayFor("getprocessstarters"),
                            delayFor("getprocessstarters"),
                            TimeUnit.SECONDS
                    );
                    Logger.info("--> GetProcessStarters worker scheduled.");
                }

                if (scheduleFor("getprocessdefinitions")) {
                    executorService.scheduleWithFixedDelay(
                            new GetProcessDefinitionsWorker(this.otelSdk, server, engineHandle),
                            initDelayFor("getprocessdefinitions"),
                            delayFor("getprocessdefinitions"),
                            TimeUnit.SECONDS
                    );
                    Logger.info("--> GetProcessDefinitions worker scheduled.");
                }

                if (scheduleFor("getactivities")) {
                    String filter = System.getProperty(
                            Constants.GETACTIVITIES_CLASSFILTER_JVMARG,
                            Constants.GETACTIVITIES_CLASSFILTER_DEFAULT
                    );
                    executorService.scheduleWithFixedDelay(
                            new GetActivitiesWorker(this.otelSdk, server, engineHandle, filter),
                            initDelayFor("getactivities"),
                            delayFor("getactivities"),
                            TimeUnit.SECONDS
                    );
                    Logger.info("--> GetActivities worker scheduled.");
                }

                Logger.info("Done scheduling the workers!");

            } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(mbs.getType())) {
                Logger.warning("Lost the bwengine's HMA MBean [" + mbs.getMBeanName() + "]");
                if (mbs.getMBeanName() == engineHandle) {
                    engineHandle = null;
                    executorService.shutdown();
                }
            }
        }
    }

    private static boolean scheduleFor(String method) {
        return Boolean.parseBoolean(
                System.getProperty(
                        Constants.METHOD_ENABLED_FLAG_JVMARG_PREFIX + "." + method + ".enabled",
                        "true"
                )
        );
    }

    private static int delayFor(String method) {
        return Integer.parseInt(
                System.getProperty(
                        Constants.METHOD_ENABLED_FLAG_JVMARG_PREFIX + "." + method + ".delay",
                        "60"
                )
        );
    }

    private static int initDelayFor(String method) {
        String defaultValue;

        /*
         * By default, avoid having all the load at the same time.
         */
        switch (method) {
            case "getexecinfo":
                defaultValue = "5";
                break;
            case "getmemoryusage":
                defaultValue = "10";
                break;
            case "getprocesscount":
                defaultValue = "15";
                break;
            case "getactiveprocesscount":
                defaultValue = "20";
                break;
            case "getprocessstarters":
                defaultValue = "25";
                break;
            case "getprocessdefinitions":
                defaultValue = "30";
                break;
            case "getactivities":
                defaultValue = "35";
                break;
            case "jvm":
                defaultValue = "40";
                break;
            default:
                defaultValue = "0";
                break;
        }

        return Integer.parseInt(
                System.getProperty(
                        Constants.METHOD_ENABLED_FLAG_JVMARG_PREFIX + ".initdelay",
                        defaultValue
                )
        );
    }
}