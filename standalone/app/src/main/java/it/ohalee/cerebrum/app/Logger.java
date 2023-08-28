package it.ohalee.cerebrum.app;

import org.slf4j.LoggerFactory;

public class Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CerebrumApplication.class);

    public static void info(String s) {
        LOGGER.info(s);
    }

    public static void warn(String s) {
        LOGGER.warn(s);
    }

    public static void warn(String s, Throwable throwable) {
        LOGGER.warn(s, throwable);
    }

    public static void severe(String s) {
        LOGGER.error(s);
    }

    public static void severe(String s, Throwable throwable) {
        LOGGER.error(s, throwable);
    }

}
