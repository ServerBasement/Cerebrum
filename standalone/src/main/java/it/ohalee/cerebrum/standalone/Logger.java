package it.ohalee.cerebrum.standalone;

import it.ohalee.basementlib.api.plugin.logging.PluginLogger;
import org.apache.logging.log4j.LogManager;

public class Logger implements PluginLogger {

    private static Logger instance;
    private final org.apache.logging.log4j.Logger logger;

    public Logger() {
        logger = LogManager.getLogger(CerebrumApplication.class);
        instance = this;
    }

    public static Logger getInstance() {
        return instance;
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void warn(String s) {
        logger.warn(s);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(s, throwable);
    }

    @Override
    public void severe(String s) {
        logger.error(s);
    }

    @Override
    public void severe(String s, Throwable throwable) {
        logger.error(s, throwable);
    }
}
