package it.ohalee.cerebrum.standalone.command.sub;

import it.ohalee.cerebrum.app.util.CerebrumError;
import it.ohalee.cerebrum.standalone.command.ArgumentCommand;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.docker.container.ServerContainer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StartAllArgument implements ArgumentCommand {

    private final DockerService dockerService;

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean value) {
        return CerebrumError.evaluate(dockerService.handle(ServerContainer::start), "All servers should start up");
    }
}
