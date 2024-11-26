package org.jaybaws.metrics.bw.util;
import java.io.FileInputStream;
import java.util.Properties;

public class BWUtils {

    private static final String cWrapperProp = "wrapper.tra.file";
    private static final String cDisplayNameProp = "Hawk.AMI.DisplayName";
    private static final String cDefaultDisplayName = "SampleDomain.SampleApplication.SampleInstance";

    public static boolean isBusinessWorksEngine() {
        String cp = System.getProperty("java.class.path", "");
        return cp.contains("/bw/5.") && cp.contains("/lib/engine.jar");
    }

    public static String getAMIDisplayName() {
        try {
            String traFile = System.getProperty(cWrapperProp);
            Properties traProps = new Properties();
            traProps.load(new FileInputStream(traFile));
            String instanceName = traProps.getProperty(cDisplayNameProp);

            return instanceName;
        } catch (Throwable t) {
            return cDefaultDisplayName;
        }
    }
}