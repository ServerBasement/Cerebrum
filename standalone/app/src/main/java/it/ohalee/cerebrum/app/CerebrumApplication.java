package it.ohalee.cerebrum.app;

import it.ohalee.cerebrum.app.commands.CerebrumCommands;
import it.ohalee.cerebrum.app.commands.TabCompletation;
import it.ohalee.cerebrum.app.integration.CommandExecutor;
import it.ohalee.cerebrum.common.loader.JarInJarClassLoader;
import it.ohalee.cerebrum.common.loader.LoaderBootstrap;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.function.Consumer;

@SpringBootApplication
public class CerebrumApplication {

    private static final String JAR_NAME = "cerebrum-standalone.jarinjar";
    private static final String BOOTSTRAP_PLUGIN_CLASS = "it.ohalee.cerebrum.standalone.CerebrumBootstrap";

    private static LoaderBootstrap plugin;
    private static CommandExecutor commandExecutor;
    @Getter
    private static boolean loaded = false;

    // Entrypoint
    public static void main(String[] args) {
        JarInJarClassLoader loader = new JarInJarClassLoader(CerebrumApplication.class.getClassLoader(), JAR_NAME);

        plugin = loader.instantiatePlugin(BOOTSTRAP_PLUGIN_CLASS, Consumer.class, o -> setCommandExecutor((CommandExecutor) o));
        plugin.onLoad();
        plugin.onEnable();

        TabCompletation.setDockerService(commandExecutor);
        CerebrumCommands.setDockerService(commandExecutor);

        loaded = true;
        SpringApplication.run(CerebrumApplication.class, args);
    }

    public static void shutdown() {
        plugin.onDisable();
        System.exit(0);
    }

    // called before start()
    public static void setCommandExecutor(CommandExecutor commandExecutor) {
        CerebrumApplication.commandExecutor = commandExecutor;
    }

}
