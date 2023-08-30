package it.ohalee.cerebrum.standalone.command.sub;

import it.ohalee.cerebrum.standalone.command.ArgumentCommand;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.docker.container.ServerContainer;
import it.ohalee.cerebrum.standalone.docker.rancher.Ranch;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class ListArgument implements ArgumentCommand {

    private final DockerService dockerService;

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean value) {
        StringBuilder builder = new StringBuilder("\n");
        if (ranch.equalsIgnoreCase("all")) {
            for (Ranch registeredRanch : dockerService.getRanches()) {
                builder.append(registeredRanch.getName()).append(":\n");
                for (ServerContainer server : registeredRanch.getServers()) {
                    if (server.getType() == ServerContainer.Type.WORKER && !server.isLoaded() && !server.isRunning())
                        continue;
                    builder.append("  ")
                            .append(server.getName().replace(registeredRanch.getName() + "_", ""))
                            .append(" (Loaded: ").append(server.isLoaded()).append(")")
                            .append(" (Running: ").append(server.isRunning()).append(")")
                            .append("\n");
                }
            }
            return builder.toString();
        }

        Optional<Ranch> optional = dockerService.getRanch(ranch);
        if (optional.isEmpty()) {
            return "Operation failed, cannot find " + ranch + " as ranch.";
        }

        builder.append(optional.get().getName()).append(": \n");
        for (ServerContainer server : optional.get().getServers()) {
            builder.append("  ")
                    .append(server.getName().replace(optional.get().getName() + "_", ""))
                    .append(" (Loaded: ").append(server.isLoaded()).append(")")
                    .append(" (Running: ").append(server.isRunning()).append(")")
                    .append("\n");
        }
        return builder.toString();
    }
}
