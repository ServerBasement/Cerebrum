package it.ohalee.cerebrum.standalone.command.sub;

import it.ohalee.cerebrum.app.CerebrumApplication;
import it.ohalee.cerebrum.standalone.command.ArgumentCommand;
import it.ohalee.cerebrum.standalone.docker.DockerService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EndArgument implements ArgumentCommand {

    private final DockerService dockerService;

    @Override
    public String execute(String arg, String ranch, String serverName, Boolean value) {
        String out = execute("stopall", ranch, serverName, value);
        return out + "\n" + execute("quit", ranch, serverName, value);
    }
}
