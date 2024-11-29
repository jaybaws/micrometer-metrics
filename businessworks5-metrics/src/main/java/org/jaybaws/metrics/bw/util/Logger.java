package org.jaybaws.metrics.bw.util;
import java.util.logging.Level;

public class Logger {

    private static volatile Logger instance = null;

    private java.util.logging.Logger logImpl = java.util.logging.Logger.getLogger(Constants.LOG_LOGGER_NAME);

    private Logger() {
        Level logLevel = Level.parse(Constants.LOG_LEVEL_DEFAULT);

        String strLogLevel = System.getProperty(Constants.LOG_LEVEL_JVMARG);
        if (strLogLevel != null) {
            try {
                logLevel = Level.parse(strLogLevel);
            } catch (Throwable t) {
                this.logImpl.warning(
                        String.format(
                                "Could not set log level to '%s'. Sticking to ''...",
                                strLogLevel,
                                Constants.LOG_LEVEL_DEFAULT
                        )
                );
            }
        }

        this.logImpl.setLevel(logLevel);
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized(Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    public final void logInfo(String msg) {
        this.logImpl.info(msg);
    }

    public final void logWarning(String msg) {
        this.logImpl.warning(msg);
    }

    public final void logWarning(String msg, Throwable t) {
        this.logImpl.log(Level.WARNING, msg, t);
    }

    public final void logSevere(String msg) {
        this.logImpl.severe(msg);
    }

    public final void logSevere(String msg, Throwable t) {
        this.logImpl.log(Level.SEVERE, msg, t);
    }

    public final void logEntering(String className, String methodName) {
        this.logImpl.entering(className, methodName);
    }

    public final void logExiting(String className, String methodName) {
        this.logImpl.exiting(className, methodName);
    }

    public final void logFine(String msg) {
        this.logImpl.fine(msg);
    }

    public static final void info(String msg) {
        Logger.getInstance().logInfo(msg);
    }

    public static final void warning(String msg) {
        Logger.getInstance().logWarning(msg);
    }

    public static final void warning(String msg, Throwable t) {
        Logger.getInstance().logWarning(msg, t);
    }

    public static final void severe(String msg) {
        Logger.getInstance().logSevere(msg);
    }

    public static final void severe(String msg, Throwable t) {
        Logger.getInstance().logSevere(msg, t);
    }

    public static final void entering(String className, String methodName) {
        Logger.getInstance().logEntering(className, methodName);
    }

    public static final void exiting(String className, String methodName) {
        Logger.getInstance().logExiting(className, methodName);
    }

    public static final void fine(String msg) {
        Logger.getInstance().logFine(msg);
    }
}