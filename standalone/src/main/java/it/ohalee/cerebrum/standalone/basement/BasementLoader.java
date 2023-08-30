package it.ohalee.cerebrum.standalone.basement;

import it.ohalee.basementlib.api.BasementLib;
import it.ohalee.basementlib.api.persistence.generic.connection.Connector;
import it.ohalee.basementlib.api.persistence.generic.connection.TypeConnector;
import it.ohalee.basementlib.api.persistence.sql.structure.AbstractSqlDatabase;
import it.ohalee.basementlib.api.plugin.logging.PluginLogger;
import it.ohalee.basementlib.common.plugin.AbstractBasementPlugin;
import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.standalone.config.CerebrumConfigAdapter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class BasementLoader {

    @Setter
    private static BasementLoader service;
    private final AbstractBasementPlugin basement;

    public BasementLoader(Path path) {
        basement = new AbstractBasementPlugin() {

            @Override
            public CerebrumConfigAdapter provideConfigurationAdapter(Class<?> clazz, File file, boolean create) {
                return new CerebrumConfigAdapter(this, BasementLoader.this.resolveConfig(clazz, file, create));
            }

            @Override
            public PluginLogger provideLogger() {
                return new PluginLogger() {
                    @Override
                    public void info(String s) {
                        Logger.info(s);
                    }

                    @Override
                    public void warn(String s) {}

                    @Override
                    public void warn(String s, Throwable t) {
                        Logger.warn(s, t);
                    }

                    @Override
                    public void severe(String s) {
                        Logger.severe(s);
                    }

                    @Override
                    public void severe(String s, Throwable t) {
                        Logger.severe(s, t);
                    }
                };
            }

            @Override
            public Path dataDirectory() {
                return path;
            }

            @Override
            public Connector createConnector(TypeConnector typeConnector, int i, int i1, String s) {
                return null;
            }

            @Override
            public AbstractSqlDatabase h2(String s) {
                return null;
            }

            @Override
            public AbstractSqlDatabase maria() {
                return null;
            }

            @Override
            public AbstractSqlDatabase database(TypeConnector typeConnector, String s) {
                return null;
            }

            @Override
            protected void registerApiOnPlatform(BasementLib basementLib) {
            }

            @Override
            protected void registerCommands() {
            }

            @Override
            protected void registerListeners() {
            }
        };

        basement.load();
        basement.enable();

        service = this;
    }

    public static BasementLib get() {
        return service.getBasement();
    }

    public static InputStream resourceStream(Class<?> clazz, String path) {
        return clazz.getClassLoader().getResourceAsStream(path);
    }

    public BasementLib getBasement() {
        return basement;
    }

    public Path resolveConfig(Class<?> clazz, File file, boolean create) {
        Path configFile = file.toPath();
        // if the config doesn't exist, create it based on the template in the resources dir
        if (create && !Files.exists(configFile)) {
            try {
                Files.createDirectories(configFile.getParent());
            } catch (IOException e) {
                // ignore
            }
            try (InputStream is = resourceStream(clazz, file.getName())) {
                Files.copy(is, configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return configFile;
    }
}
