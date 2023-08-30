package it.ohalee.cerebrum.standalone.command.sub;

import it.ohalee.cerebrum.app.util.CerebrumError;
import it.ohalee.cerebrum.standalone.command.ArgumentCommand;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.docker.rancher.Ranch;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class StopArgument implements ArgumentCommand {

    private final DockerService dockerService;

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean value) {
        Optional<Ranch> optRanch = dockerService.getRanch(ranch);
        if (optRanch.isEmpty()) {
            return "Operation failed, cannot find " + ranch + " as ranch.";
        }

        if (serverName.equalsIgnoreCase("all")) {
            return CerebrumError.evaluate(optRanch.get().shutdown(), "The " + ranch + " ranch servers are shutting down");
        }

        return CerebrumError.evaluate(dockerService.stopServer(ranch, serverName),
                "Server " + ranch + "." + serverName + " is shutting down");
    }
}
