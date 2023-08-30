package it.ohalee.cerebrum.app;

import org.slf4j.LoggerFactory;

public class Logger {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CerebrumApplication.class);

    public static void info(String s) {
        if (CerebrumApplication.isLoaded()) {
            LOGGER.info(s);
        } else {
            System.out.println(s);
        }
    }

    public static void warn(String s) {
        if (CerebrumApplication.isLoaded()) {
            LOGGER.warn(s);
        } else {
            System.out.println(s);
        }
    }

    public static void warn(String s, Throwable throwable) {
        if (CerebrumApplication.isLoaded()) {
            LOGGER.warn(s, throwable);
        } else {
            System.out.println(s);
        }
    }

    public static void severe(String s) {
        if (CerebrumApplication.isLoaded()) {
            LOGGER.error(s);
        } else {
            System.out.println(s);
        }
    }

    public static void severe(String s, Throwable throwable) {
        if (CerebrumApplication.isLoaded()) {
            LOGGER.error(s, throwable);
        } else {
            System.out.println(s);
        }
    }

}
