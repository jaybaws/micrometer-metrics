package org.jaybaws.metrics.bw.util;
import java.util.logging.Level;

public class Logger {

    private static volatile Logger instance = null;

    private java.util.logging.Logger logImpl = java.util.logging.Logger.getLogger(Constants.LOG_LOGGER_NAME);
    private boolean traceToStdOut = false;

    private Logger() {
        Level logLevel = Level.parse(Constants.LOG_LEVEL_DEFAULT);
        this.traceToStdOut = System.getenv().containsKey("ORG_JAYBAWS_CONSOLETRACE");

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

    private final void log(Level lvl, String msg) {
        this.logImpl.log(lvl, msg);
        if (this.traceToStdOut) {
            System.out.println(String.format("[%s] - %s", lvl.toString(), msg));
        }
    }

    private final void log(Level lvl, String msg, Throwable t) {
        this.logImpl.log(lvl, msg, t);
        if (this.traceToStdOut) {
            System.out.println(String.format("[%s] - %s - %s", lvl.toString(), msg, t.toString()));
        }
    }

    public static void info(String msg) {
        Logger.getInstance().log(Level.INFO, msg);
    }

    public static void warning(String msg) {
        Logger.getInstance().log(Level.WARNING, msg);
    }

    public static void warning(String msg, Throwable t) {
        Logger.getInstance().log(Level.WARNING, msg, t);
    }

    public static void severe(String msg) {
        Logger.getInstance().log(Level.SEVERE, msg);
    }

    public static void severe(String msg, Throwable t) {
        Logger.getInstance().log(Level.SEVERE, msg, t);
    }

    public static void entering(String className, String methodName) {
        Logger.getInstance().log(Level.FINEST, String.format("Entering %s in %s.", className, methodName));
    }

    public static void exiting(String className, String methodName) {
        Logger.getInstance().log(Level.FINEST, String.format("Exiting %s in %s.", className, methodName));
    }

    public static void fine(String msg) {
        Logger.getInstance().log(Level.FINE, msg);
    }
}