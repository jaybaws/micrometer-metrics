package org.jaybaws.metrics.bw.util;

public class Constants {

    public static final String LOG_LEVEL_JVMARG = "org.jbaws.loglevel";
    public static final String LOG_LOGGER_NAME = "org.jbaws";
    public static final String LOG_LEVEL_DEFAULT = "ALL";

    public static final String GETACTIVITIES_CLASSFILTER_JVMARG = "org.jaybaws.getactivities.classfilter";
    public static final String GETACTIVITIES_CLASSFILTER_DEFAULT = ".*(SOAP|JMS|JDBC|http|FTP|GenerateErrorActivity|CatchActivity|JavaActivity|MapperActivity).*";

    public static final String METHOD_ENABLED_FLAG_JVMARG_PREFIX = "org.jaybaws.method";

    public static final int EXECUTORSERVICE_CORE_POOLSIZE = 10;

}