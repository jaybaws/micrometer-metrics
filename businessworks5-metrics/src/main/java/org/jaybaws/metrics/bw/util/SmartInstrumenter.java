package org.jaybaws.metrics.bw.util;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SmartInstrumenter {

    /* We use this system property to determine the application .TRA file.
       The presence of this system property indicates we're instrumenting a bwengine.
     */
    public static final String cWrapperProp = "wrapper.tra.file";

    /* The property in the application's .tra file to use for determining
       the instance name, and it's corresponding default App/Tier/Node
     */
    private static final String cDisplayNameProp = "Hawk.AMI.DisplayName";

    private static final Logger LOGGER = Logger.getLogger(SmartInstrumenter.class.getName());

    public static void prepare(String type) {
        switch (type) {
            case "":
                break;
            case "azure-application-insights":
                prepareAzureApplicationInsights();
                break;
            default:
                LOGGER.log(Level.WARNING, String.format("Provided type `%s` not recognized. Valid arguments are ['azure-application-insights']. No smart instrumentation will be done!", type));
                break;
        }
    }

    private static void prepareAzureApplicationInsights() {
        /* The JVM property that tells the actual Azure Monitor java agent where to look for the configuration. */
        final String cAzureMonitorConfigFilePathProp = "applicationinsights.configuration.file";

        /* The JVM property that tells the actual Azure Monitor java agent what cloudRoleName our app has */
        final String cAzureMonitorRoleNameProp = "applicationinsights.role.name";

        /* The JVM property that tells the actual Azure Monitor java agent what cloudRoleInstance our app has */
        final String cAzureMonitorRoleInstanceProp = "applicationinsights.role.instance";

        boolean doInstrument = BWUtils.isBusinessWorksEngine();

        String givenConfigFile = System.getProperty(cAzureMonitorConfigFilePathProp);

        LOGGER.finest("Starting javaagent to customize the HV AppDynamics environment ");

        if (givenConfigFile != null) { // In case developers provide their own config file, we don't want to crash their party!
            if (new File(givenConfigFile).isFile()) { // The file should exist though...
                doInstrument = false;
                LOGGER.finest("ApplicationInsights configuration file provided upfront! Taking that one!");
            } else { // Bad developer, set your properties correct!
                LOGGER.log(
                        Level.FINEST,
                        "ApplicationInsights configuration file [{0}] provided upfront, but it does not exist! UNSETTING IT! Proceeding with settings safe defaults!",
                        new Object[]{givenConfigFile}
                );

                System.clearProperty(cAzureMonitorConfigFilePathProp); // Clear the property to avoid ApplicationInsights agent to fail!
            }
        }

        if (doInstrument) { // So we actually need to do something
            try {
                String traFile = System.getProperty(cWrapperProp);
                LOGGER.log(
                        Level.FINE,
                        "Determined .tra file using property [{0}]: {1}.",
                        new Object[]{cWrapperProp, traFile}
                );

                if (traFile == null) { // Avoid injecting crap into other JVMs than bwengines
                    LOGGER.fine("No .tra file detected, so likely not a bwengine... Leaving!");
                } else { // Here's where the magic happens
                    Properties traProps = new Properties();
                    traProps.load(new FileInputStream(traFile));
                    LOGGER.log(
                            Level.FINE,
                            "Specific deployment's .tra file [{0}] loaded.",
                            new Object[]{traFile}
                    );

                    String instanceName = traProps.getProperty(cDisplayNameProp);
                    LOGGER.log(
                            Level.FINE,
                            "Determine adapter instance name based on property [{0}]: {1}.",
                            new Object[]{cDisplayNameProp, instanceName}
                    );

                    if (instanceName != null) {
                        String[] instanceNameParts = instanceName.split("\\.");

                        if (instanceNameParts.length >= 7) {
                        /* Parse default values, assuming instanceName looks like:
                           COM.TIBCO.ADAPTER.bwengine.<domain>.<application>.<process-archive>
                        */
                            String domain = instanceNameParts[4];  // corresponds to <domain>
                            String application = instanceNameParts[5]; // corresponds to <application>
                            String archive = instanceNameParts[6]; // corresponds to <process-archive>

                            LOGGER.log(
                                    Level.FINEST,
                                    "Determined actual AppDynamics values [domain: {0}, application: {1}, archive: {2}].",
                                    new Object[]{domain, application, archive}
                            );

                            String configFile = "/data/adapter/" + domain + "/extresources/" + application + "/monitor/applicationinsights.json";

                            if (new File(configFile).isFile()) { // If developer shipped a config file in the expected place, use it!
                                System.setProperty(cAzureMonitorConfigFilePathProp, configFile);

                                LOGGER.log(
                                        Level.INFO,
                                        "==> Pointing to the right config file by setting JVM property [{0}] to [{1}].",
                                        new Object[]{cAzureMonitorConfigFilePathProp, configFile}
                                );
                            } else { // Fall back to safe defaults if no such config file exists on disk
                                System.setProperty(cAzureMonitorRoleNameProp, application);
                                System.setProperty(cAzureMonitorRoleInstanceProp, archive);

                                LOGGER.log(
                                        Level.INFO,
                                        "==> config file not found on the expected place [{0}], so only injecting roleName [{1}] and instanceName [{2}].",
                                        new Object[]{configFile, application, archive}
                                );
                            }
                        } else {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Could not determine defaults based on [{0}]'s value [{1}].",
                                    new Object[]{cDisplayNameProp, instanceName}
                            );
                        }
                    } else {
                        LOGGER.log(
                                Level.SEVERE,
                                "Could not determine settings because property [{0}] returned a null value.",
                                new Object[]{cDisplayNameProp}
                        );
                    }
                }
            } catch (Throwable t) {
                LOGGER.log(
                        Level.SEVERE,
                        "Something went horribly wrong!",
                        t
                );
            }
        }
    }
}