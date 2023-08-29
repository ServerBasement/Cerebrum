package it.ohalee.cerebrum.standalone.command.sub;

import it.ohalee.cerebrum.app.CerebrumApplication;
import it.ohalee.cerebrum.standalone.command.ArgumentCommand;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import it.ohalee.cerebrum.standalone.docker.container.ServerContainer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuitArgument implements ArgumentCommand {

    private final DockerService dockerService;

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean value) {
        DockerService.getExecutor().shutdown();
        CerebrumApplication.shutdown();
        return "The application is shutting down";
    }
}
