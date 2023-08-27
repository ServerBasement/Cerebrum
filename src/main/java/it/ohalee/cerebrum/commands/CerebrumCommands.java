package it.ohalee.cerebrum.commands;

import it.ohalee.cerebrum.CerebrumApplication;
import it.ohalee.cerebrum.Logger;
import it.ohalee.cerebrum.docker.DockerService;
import it.ohalee.cerebrum.docker.container.ServerContainer;
import it.ohalee.cerebrum.docker.rancher.Ranch;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@ShellComponent
public class CerebrumCommands {

    private static DockerService dockerService;

    public static void setDockerService(DockerService service) {
        dockerService = service;
    }

    @ShellMethod("Starts a container and its server")
    public void start(@ShellOption(value = "-r", valueProvider = TabCompletation.class) String ranch, @ShellOption(value = "-s", valueProvider = TabCompletation.class, defaultValue = "all") String serverName) {
        Logger.getInstance().info("Start command issued");
        if (serverName.equalsIgnoreCase("all")) {
            Optional<Ranch> optRanch = dockerService.getRanch(ranch);
            if (optRanch.isPresent()) {
                optRanch.get().startLeaders();
            } else Logger.getInstance().warn("Operation failed, cannot find " + ranch + " as ranch.");
            return;
        }
        dockerService.startServer(ranch, serverName, false);
    }

    @ShellMethod("Stops a container and its server")
    public void stop(@ShellOption(value = "-r", valueProvider = TabCompletation.class) String ranch, @ShellOption(value = "-s", valueProvider = TabCompletation.class, defaultValue = "all") String serverName) {
        Logger.getInstance().info("Stop command issued");
        if (serverName.equalsIgnoreCase("all")) {
            Optional<Ranch> optRanch = dockerService.getRanch(ranch);
            if (optRanch.isPresent()) {
                optRanch.get().shutdown();
            } else Logger.getInstance().warn("Operation failed, cannot find " + ranch + " as ranch.");
            return;
        }
        dockerService.stopServer(ranch, serverName);
    }

    @ShellMethod("Stops all containers")
    public String stopAll() {
        Logger.getInstance().info("Stop-all command issued");
        handle(ServerContainer::stop);
        return "All servers (may be) stopped";
    }

    @ShellMethod("Starts all containers")
    public String startAll() {
        Logger.getInstance().info("Start-all command issued");
        handle(ServerContainer::start);
        return "All servers (may be) up and running";
    }

    @ShellMethod("Stops cerebrum but not containers")
    public String quit() {
        Logger.getInstance().info("Quit command issued");
        DockerService.getExecutor().shutdown();
        CerebrumApplication.exit();
        return "Bye Bye!";
    }

    @ShellMethod("Stops cerebrum and all the containers")
    public String end() {
        Logger.getInstance().info("End command issued");
        String out = stopAll();
        return out + "\n" + quit();
    }

    @ShellMethod("Reload configurations")
    public String reload() {
        Logger.getInstance().info("Reload command issued");
        dockerService.recalculateConfiguration();
        return "The ConfigurationAdapter (may be) reloaded";
    }

    @ShellMethod("Lists all containers")
    public String list(@ShellOption(defaultValue = "all", valueProvider = TabCompletation.class) String ranch) {
        Logger.getInstance().info("List command issued");
        StringBuilder builder = new StringBuilder("\n");
        if (ranch.equalsIgnoreCase("all")) {
            for (Ranch registeredRanch : dockerService.getRanches()) {
                builder.append(registeredRanch.getName()).append(" ->\n");
                for (ServerContainer server : registeredRanch.getServers()) {
                    if (server.getType() == ServerContainer.Type.WORKER && !server.isLoaded() && !server.isRunning()) continue;
                    builder.append("  ")
                            .append(server.getName().replace(registeredRanch.getName() + "_", ""))
                            .append(" ( Loaded: ").append(server.isLoaded()).append(" )")
                            .append(" ( Running: ").append(server.isRunning()).append(" )")
                            .append("\n");
                }
            }
            return builder.toString();
        }
        Optional<Ranch> optional = dockerService.getRanch(ranch);
        if (optional.isPresent()) {
            builder.append(optional.get().getName()).append(" ->\n");
            for (ServerContainer server : optional.get().getServers()) {
                builder.append("  ")
                        .append(server.getName().replace(optional.get().getName() + "_", ""))
                        .append(" ( Loaded: ").append(server.isLoaded()).append(" )")
                        .append(" ( Running: ").append(server.isRunning()).append(" )")
                        .append("\n");
            }
        } else {
            Logger.getInstance().warn("Operation failed. Cannot find ranch " + ranch + ".");
        }
        return builder.toString();
    }

    @ShellMethod("Send file update")
    public void update() {
        dockerService.updateJars();
    }

    private void handle(Consumer<ServerContainer> consumer) {
        for (Ranch ranch : dockerService.getRanches())
            for (ServerContainer server : ranch.getServers())
                consumer.accept(server);
    }

}
