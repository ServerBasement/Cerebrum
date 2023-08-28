package it.ohalee.cerebrum.standalone.command;

import it.ohalee.cerebrum.app.integration.CommandExecutor;
import it.ohalee.cerebrum.app.Logger;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.docker.container.ServerContainer;
import it.ohalee.cerebrum.standalone.docker.rancher.Ranch;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StandaloneCommandManager implements CommandExecutor {

    private final DockerService dockerService;

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean b) {
        switch (arg) {
            case "start" -> {
                Logger.info("Start command issued");
                if (serverName.equalsIgnoreCase("all")) {
                    Optional<Ranch> optRanch = dockerService.getRanch(ranch);
                    if (optRanch.isPresent()) {
                        optRanch.get().startLeaders();
                    } else Logger.warn("Operation failed, cannot find " + ranch + " as ranch.");
                    return null;
                }
                dockerService.startServer(ranch, serverName, false);
            }
            case "stop" -> {
                Logger.info("Stop command issued");
                if (serverName.equalsIgnoreCase("all")) {
                    Optional<Ranch> optRanch = dockerService.getRanch(ranch);
                    if (optRanch.isPresent()) {
                        optRanch.get().shutdown();
                    } else Logger.warn("Operation failed, cannot find " + ranch + " as ranch.");
                    return null;
                }
                dockerService.stopServer(ranch, serverName);
            }
            case "stopall" -> {
                Logger.info("Stop-all command issued");
                handle(ServerContainer::stop);
                return "All servers (may be) stopped";
            }
            case "startall" -> {
                Logger.info("Start-all command issued");
                handle(ServerContainer::start);
                return "All servers (may be) up and running";
            }
            case "quit" -> {
                Logger.info("Quit command issued");
                DockerService.getExecutor().shutdown();
                System.exit(0);
                return "Bye Bye!";
            }
            case "end" -> {
                Logger.info("End command issued");
                String out = execute("stopall", ranch, serverName, b);
                return out + "\n" + execute("quit", ranch, serverName, b);
            }
            case "reload" -> {
                Logger.info("Reload command issued");
                dockerService.recalculateConfiguration();
                return "The ConfigurationAdapter (may be) reloaded";
            }
            case "list" -> {
                Logger.info("List command issued");
                StringBuilder builder = new StringBuilder("\n");
                if (ranch.equalsIgnoreCase("all")) {
                    for (Ranch registeredRanch : dockerService.getRanches()) {
                        builder.append(registeredRanch.getName()).append(" ->\n");
                        for (ServerContainer server : registeredRanch.getServers()) {
                            if (server.getType() == ServerContainer.Type.WORKER && !server.isLoaded() && !server.isRunning())
                                continue;
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
                    Logger.warn("Operation failed. Cannot find ranch " + ranch + ".");
                }
                return builder.toString();
            }
            case "update" ->  {
                dockerService.updateJars();
            }
        }
        return null;
    }

    private void handle(Consumer<ServerContainer> consumer) {
        for (Ranch ranch : dockerService.getRanches())
            for (ServerContainer server : ranch.getServers())
                consumer.accept(server);
    }

    @Override
    public List<String> tabCompletion(String currentWord, List<String> words, String currentWordUpToCursor) {
        switch (currentWord) {
            case "ranch" -> {
                return dockerService.getRegisteredRanches().parallelStream()
                        .filter(ranch -> ranch.startsWith(currentWordUpToCursor))
                        .toList();
            }
            case "serverName" -> {
                Optional<Ranch> optRanch = dockerService.getRanch(words.get(1));
                if (optRanch.isEmpty())
                    return Collections.emptyList();
                String ranchName = optRanch.get().getName();
                return optRanch.get().getServers().parallelStream()
                        .map(container -> container.getName().replace(ranchName + "_", ""))
                        .filter(s -> s.startsWith(currentWordUpToCursor))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
