package it.ohalee.cerebrum.standalone.command.sub;

import it.ohalee.cerebrum.app.util.CerebrumError;
import it.ohalee.cerebrum.standalone.command.ArgumentCommand;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateArgument implements ArgumentCommand {

    private final DockerService dockerService;

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean value) {
        return CerebrumError.evaluate(dockerService.updateJars(), "Jars updated");
    }
}
